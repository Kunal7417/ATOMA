package com.atoma.marketplace.onboarding;

import com.atoma.marketplace.auth.dto.AuthDtos;
import com.atoma.marketplace.auth.dto.MerchantAuthDtos;
import com.atoma.marketplace.common.enums.OtpDeliveryChannel;
import com.atoma.marketplace.product.entity.Category;
import com.atoma.marketplace.product.repository.CategoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OnboardingConfigLocalizationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void configLocalizedForDari() throws Exception {
        var token = otpSignIn();
        var body = mockMvc.perform(get("/api/v1/onboarding/config")
                        .header("Authorization", "Bearer " + token)
                        .header("Accept-Language", "fa"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.steps[0].title").value("جزئیات کسب‌وکار"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertTrue(body.contains("مواد غذایی و خوراکه"), "expected localized category name in response");
    }

    private String otpSignIn() throws Exception {
        var phone = "+9370" + String.format("%07d", Math.abs(UUID.randomUUID().hashCode() % 10_000_000));
        var request = new MerchantAuthDtos.OtpRequest(OtpDeliveryChannel.SMS, phone, null);
        var send = mockMvc.perform(post("/api/v1/auth/otp/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        var requestId = objectMapper.readTree(send.getResponse().getContentAsString()).get("requestId").asText();
        var verify = new AuthDtos.OtpVerifyBody(UUID.fromString(requestId), "000000", null, null, null);
        var auth = mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(auth.getResponse().getContentAsString())
                .get("tokens").get("accessToken").asText();
    }
}
