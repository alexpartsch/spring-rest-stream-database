package com.github.shwocase.spring.stream_db2rest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.OutputStream;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
public class StreamingService implements ApplicationListener<ContextRefreshedEvent> {

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

    @Override
    @Transactional
    public void onApplicationEvent(ContextRefreshedEvent event) {
        // Initialize with some random test data
        var faker = new Faker();
        var testEntities = IntStream.range(1,10_000)
                .mapToObj(i -> new FakeDataEntity(faker.starTrek().character(),
                        faker.lordOfTheRings().character(),
                        faker.buffy().characters()))
                .collect(Collectors.toList());
        repository.saveAll(testEntities);
    }
}
