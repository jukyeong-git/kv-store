package io.github.jukyeong.kvstore.exception;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Request with no Content-Type should return 415")
    void request_noContentType_returns415() throws Exception {
        mockMvc.perform(post("/object")
                        .content("{\"key\": \"k1\", \"value\": \"v1\"}"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    @DisplayName("Unknown endpoint should return 404")
    void request_unknownEndpoint_returns404() throws Exception {
        mockMvc.perform(get("/unknown"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("Error response should contain error field")
    void request_error_responseContainsErrorField() throws Exception {
        mockMvc.perform(get("/object/nonexistentkey"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("Validation error response should contain error field")
    void request_validationError_responseContainsErrorField() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").exists());
    }
}
