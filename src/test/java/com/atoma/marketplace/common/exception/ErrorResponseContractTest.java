package com.atoma.marketplace.common.exception;

import com.atoma.marketplace.auth.dto.MerchantAuthDtos;
import com.atoma.marketplace.common.enums.OtpDeliveryChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ErrorResponseContractTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void otpInvalidReturnsStandardErrorShape() throws Exception {
        var phone = "+9370" + String.format("%07d", Math.abs(UUID.randomUUID().hashCode() % 10_000_000));
        var request = new MerchantAuthDtos.OtpRequest(OtpDeliveryChannel.SMS, phone, null);
        var send = mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        var requestId = objectMapper.readTree(send.getResponse().getContentAsString()).get("requestId").asText();

        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + requestId + "\",\"code\":\"999999\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("OTP_INVALID"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.details.attemptsLeft").isNumber())
                .andExpect(jsonPath("$.timestamp").doesNotExist())
                .andExpect(jsonPath("$.attemptsRemaining").doesNotExist());
    }
}
