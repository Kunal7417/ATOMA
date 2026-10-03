package com.atoma.marketplace.auth;

import com.atoma.marketplace.auth.dto.AuthDtos;
import com.atoma.marketplace.auth.dto.MerchantAuthDtos;
import com.atoma.marketplace.common.enums.OtpChannel;
import com.atoma.marketplace.common.enums.OtpDeliveryChannel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
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
class OtpAuthIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void resendRequiresPriorRequest() throws Exception {
        mockMvc.perform(post("/api/v1/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("OTP_REQUEST_NOT_FOUND"));
    }

    @Test
    void mobileOtpRequestResendVerify() throws Exception {
        var phone = "+9370" + String.format("%07d", Math.abs(UUID.randomUUID().hashCode() % 10_000_000));
        var request = new MerchantAuthDtos.OtpRequest(OtpDeliveryChannel.SMS, phone, null);

        var sendResult = mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").exists())
                .andReturn();

        var requestId = objectMapper.readTree(sendResult.getResponse().getContentAsString())
                .get("requestId").asText();

        var resendResult = mockMvc.perform(post("/api/v1/auth/otp/resend")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"" + requestId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").exists())
                .andReturn();
        requestId = objectMapper.readTree(resendResult.getResponse().getContentAsString())
                .get("requestId").asText();

        var verify = new AuthDtos.OtpVerifyBody(
                UUID.fromString(requestId),
                "000000",
                null,
                null,
                null
        );
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokens.accessToken").exists())
                .andExpect(jsonPath("$.tokens.refreshToken").exists())
                .andExpect(jsonPath("$.nextRoute").value("ONBOARDING_BUSINESS"));
    }

    @Test
    void legacySendAndVerify() throws Exception {
        var phone = "+9370" + String.format("%07d", Math.abs(UUID.randomUUID().hashCode() % 10_000_000));
        var send = new AuthDtos.OtpSendRequest(OtpChannel.PHONE, phone);
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(send)))
                .andExpect(status().isOk());

        var verify = new AuthDtos.OtpVerifyBody(null, "000000", null, OtpChannel.PHONE, phone);
        mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
    }
}
