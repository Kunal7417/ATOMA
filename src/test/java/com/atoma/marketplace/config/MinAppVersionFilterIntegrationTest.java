package com.atoma.marketplace.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "atoma.app.enforce-version-header=true",
        "atoma.app.min-merchant-version=0.1.0"
})
class MinAppVersionFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void rejectsClientBelowMinimumVersion() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .header("X-App-Version", "0.0.1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"SMS\",\"phone\":\"+93701234567\"}"))
                .andExpect(status().isUpgradeRequired())
                .andExpect(jsonPath("$.code").value("APP_UPDATE_REQUIRED"))
                .andExpect(jsonPath("$.details.minVersion").value("0.1.0"));
    }

    @Test
    void rejectsMissingVersionHeader() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"SMS\",\"phone\":\"+93701234568\"}"))
                .andExpect(status().isUpgradeRequired())
                .andExpect(jsonPath("$.code").value("APP_UPDATE_REQUIRED"));
    }

    @Test
    void acceptsCurrentMerchantAppVersion() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/request")
                        .header("X-App-Version", "0.1.0")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"SMS\",\"phone\":\"+93707000001\"}"))
                .andExpect(status().isOk());
    }
}
