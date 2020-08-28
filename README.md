# Spring - Stream HTTP Response from JPA

[Alexander Partsch <alexander@partsch.ninja>](mailto:alexander@partsch.ninja?subject=[GitHub]%20Spring%20Stream%20Response)
| 28th of August, 2020 - 06:00 in the Morning | [@wtfjohngalt on Twitter](https://twitter.com/wtfjohngalt)

While working on a web service sending 10.000+ records to a client for further
processing, we realized the connection and handling logic of classical
pagination would impose an untenable performance penalty on the user. We knew
about [JSON Streaming](https://en.wikipedia.org/wiki/JSON_streaming) as an
alternative, but never used it up until know. In the following this text
explains how to implement a streaming HTTP endpoint handler with the Spring
Framework. We'll even stream records fetched via JPA and see how the connection
handling is ensured.

## StreamingResponseBody

The Spring MVC class [StreamingResponseBody](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/web/servlet/mvc/method/annotation/StreamingResponseBody.html)
is a functional interface for asynchronous request processing. Even simpler:
It's a `Consumer` of an `OutputStream` one returns in a Spring MVC request
handler. The `OutputStream` points to the Servlets request body.

```java
@RestController
class StreamingResponseBodyExample {
    @GetMapping(path = "/")
    StreamingResponseBody stream() {
        return out -> {
            try (var writer = new PrintWriter(out)) {
                writer.println("Hello, World!");
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
```

## Streaming JSON

When using `StreamingResponseBody` one cannot expect Spring to serialize the
response body to the desired representation format anymore, since the handler
writes directly to the Servlets response `OutputStream`. Therefore I would
employ [JsonFactory](https://fasterxml.github.io/jackson-core/javadoc/2.5/com/fasterxml/jackson/core/JsonFactory.html)
with any `ObjectMapper` of choice as codec to create a [JsonGenerator](https://fasterxml.github.io/jackson-core/javadoc/2.5/com/fasterxml/jackson/core/JsonGenerator.html),
which can be used to serialize on the fly. In the following example a list of
POJOs is converted to a JSON array in the response body:

```java
@RestController
class StreamingJsonExample {

    private static final JsonFactory JSON_FACTORY =
        new JsonFactory().setCodec(new ObjectMapper());

    @Autowired
    private DataService service;

    @GetMapping(path = "/", 
        produces = {MediaType.APPLICATION_STREAM_JSON_VALUE}) // 1
    StreamingResponseBody stream() {
        return out -> {
            var data = service.loadData();
            try (var json = JSON_FACTORY.createGenerator(out)) {
                json.writeStartArray();
                for(var item : data) {
                    json.writeObject(item);
                 }
                json.writeEndArray();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        };
    }
}
```

**//1** We need to specify the response body ourselves.

## Connecting to JPA

Springs JPA implementation supports [streaming query results](https://docs.spring.io/spring-data/jpa/docs/current/reference/html/#repositories.query-streaming),
if the datasource allows so. This means the repository methods return a 
`java.util.Stream` instead of a collection. Records are fetched as demanded.
Since the `StreamingResponseBody` is executed after our MVC request handler
returned, we need to make a sure the transaction is kept alive until the 
stream completed.

Therefore Spring defined the [OpenEntityManagerInViewFilter](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/orm/jpa/support/OpenEntityManagerInViewFilter.html#:~:text=Intended%20for%20the%20%22Open%20EntityManager,be%20autodetected%20by%20transaction%20managers.)
providing the database connection / transaction to any newly spawned thread and
properly cleaning up afterwards. We just have to make sure the response body is
executed under a transaction. We can easily achive this with a service layer
class that applies a `@Transactional` annotation (make sure neither class
nor method are final or non-public):

````java
@Service
public class StreamingService {

    private static final JsonFactory JSON_FACTOR = new JsonFactory()
            .setCodec(new ObjectMapper());

    @Autowired
    private FakeDataRepository repository;

    @Transactional(readOnly = true)
    public void stream(OutputStream outputStream) throws IOException {
        try(var json = JSON_FACTOR.createGenerator(outputStream)) {
            json.writeStartArray();
            repository.findByIdNotNull()
                    .forEach(pojo -> {
                        try {
                            json.writeObject(pojo);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
            json.writeEndArray();
        }
    }
}
````

The controller is simply referencing it:

````java
@RestController
@SpringBootApplication
public class SpringRestStreamFromDatabaseApplication {

	@Autowired
	private StreamingService streamingService;

	public static void main(String[] args) {
		SpringApplication.run(SpringRestStreamFromDatabaseApplication.class, args);
	}

	@GetMapping(path = "/stream", produces = {MediaType.APPLICATION_STREAM_JSON_VALUE})
	public StreamingResponseBody stream() {
		return streamingService::stream;
	}

}
````

The JPA repository is even simpler:

```java
@Repository
public interface FakeDataRepository extends JpaRepository<FakeDataEntity, UUID> {

    Stream<FakeDataEntity> findByIdNotNull();

}
```

## Testing Streaming Endpoints

Testing streaming endpoints with `MockMvc` differs since you need to keep
the connection/request handler alive until the whole response was red. Spring
therefore offers the `RequestBuilder` [asyncDispatch](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/test/web/servlet/request/MockMvcRequestBuilders.html#asyncDispatch-org.springframework.test.web.servlet.MvcResult-)
to wait for the connection to close:

````java
@SpringBootTest
@ActiveProfiles({"test"})
class SpringRestStreamFromDatabaseApplicationTests {

    private MockMvc api;

    @Autowired
    @BeforeEach
    void setup(WebApplicationContext wac) {
        api = MockMvcBuilders.webAppContextSetup(wac)
                .build();
    }

    @Test
    @DisplayName("Stream Response from Database")
    void shouldStreamResponseFromDatabase() throws Exception {
        // Act
        var async = api.perform(get("/stream"))
                .andExpect(request().asyncStarted())
                .andDo(MvcResult::getAsyncResult)
                .andReturn();
        api.perform(asyncDispatch(async))
                .andDo(print())
                .andExpect(status().isOk());

    }

}
````

## Connection Pool Leak < Spring Boot 2.3

As you can read in [this issue](https://github.com/spring-projects/spring-boot/issues/15794),
there was a connection leakage bug in [OpenEntityManagerInViewFilter](https://docs.spring.io/spring/docs/current/javadoc-api/org/springframework/orm/jpa/support/OpenEntityManagerInViewFilter.html#:~:text=Intended%20for%20the%20%22Open%20EntityManager,be%20autodetected%20by%20transaction%20managers.)
not properly dispatching the threads and JPA transactins. This can be fixed
by registering the filter anew with the `ASYNC` `DispatchType`:

```java
 @Test 
 void startupWithOncePerRequestDefaults() throws Exception { 
 	FilterRegistrationBean<?> bean = new FilterRegistrationBean<>(this.oncePerRequestFilter); 
 	bean.onStartup(this.servletContext); 
 	verify(this.servletContext).addFilter(eq("oncePerRequestFilter"), eq(this.oncePerRequestFilter)); 
 	verify(this.registration).setAsyncSupported(true); 
 	verify(this.registration).addMappingForUrlPatterns(EnumSet.allOf(DispatcherType.class), false, "/*"); 
 } 
```

## Conclusion

Support for JSON Streaming in Spring comes nearly out of the box. Using the
`JsonGenerator` and knowing about the `OpenEntityManagerInView` pattern is
all you need to move on. A complexity decrease in your client code as well as
performance improvements should appply if done correctly.