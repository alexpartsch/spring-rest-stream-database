package com.github.shwocase.spring.stream_db2rest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
