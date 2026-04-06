package io.github.jukyeong.kvstore.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
class KvControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("POST /object with valid request should return 200")
    void save_validRequest_returns200() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"testkey\", \"value\": \"value1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.key").value("testkey"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.createdAtFormatted").exists());
    }

    @Test
    @DisplayName("POST /object with missing key should return 400")
    void save_missingKey_returns400() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"value\": \"test\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /object with missing value should return 400")
    void save_missingValue_returns400() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"testkey\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /object with empty body should return 400")
    void save_emptyBody_returns400() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /object with invalid JSON should return 400")
    void save_invalidJson_returns400() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("invalid-json"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /object/{key} should return latest version")
    void get_existingKey_returnsLatest() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"ctrlkey\", \"value\": \"value1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/object/ctrlkey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1))
                .andExpect(jsonPath("$.key").value("ctrlkey"));
    }

    @Test
    @DisplayName("GET /object/{key} with unknown key should return 404")
    void get_unknownKey_returns404() throws Exception {
        mockMvc.perform(get("/object/unknownkey"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").exists());
    }

    @Test
    @DisplayName("GET /object/{key}?timestamp with past timestamp should return 404")
    void get_pastTimestamp_returns404() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"tskey\", \"value\": \"value1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/object/tskey?timestamp=1000"))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("GET /object/get_all_records should return JSON array")
    void getAll_returnsJsonArray() throws Exception {
        mockMvc.perform(get("/object/get_all_records"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("GET response version should be integer type")
    void get_response_versionIsInteger() throws Exception {
        mockMvc.perform(post("/object")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"key\": \"intkey\", \"value\": \"v1\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/object/intkey"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").isNumber());
    }
}