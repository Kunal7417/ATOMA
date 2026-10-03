package com.atoma.marketplace.onboarding;

import com.atoma.marketplace.auth.dto.AuthDtos;
import com.atoma.marketplace.auth.dto.MerchantAuthDtos;
import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.enums.OtpDeliveryChannel;
import com.atoma.marketplace.onboarding.dto.OnboardingApiDtos;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantOnboardingApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Test
    void configAndBusinessStepFlow() throws Exception {
        var categoryId = seedCategory();
        var token = otpSignIn();

        mockMvc.perform(get("/api/v1/onboarding/config")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.businessTypes").isArray());

        mockMvc.perform(get("/api/v1/onboarding/application")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(0));

        mockMvc.perform(patch("/api/v1/onboarding/application/business")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OnboardingApiDtos.PatchBusinessRequest(
                                0L,
                                BusinessType.REGISTERED_SHOP,
                                "Test Shop",
                                categoryId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version").value(1));

        mockMvc.perform(post("/api/v1/onboarding/application/business/submit")
                        .header("Authorization", "Bearer " + token)
                        .header("Idempotency-Key", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OnboardingApiDtos.SubmitBusinessRequest(
                                1L,
                                BusinessType.REGISTERED_SHOP,
                                "Test Shop",
                                categoryId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nextStep").value("OWNER"))
                .andExpect(jsonPath("$.application.version").value(2));
    }

    private UUID seedCategory() {
        var name = "Cat-" + UUID.randomUUID().toString().substring(0, 8);
        var slug = name.toLowerCase().replace(' ', '-');
        return categoryRepository.save(Category.builder()
                .name(name)
                .slug(slug)
                .active(true)
                .sortOrder(1)
                .build()).getId();
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
