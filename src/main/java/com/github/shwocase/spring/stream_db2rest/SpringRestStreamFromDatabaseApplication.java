package com.github.shwocase.spring.stream_db2rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

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
