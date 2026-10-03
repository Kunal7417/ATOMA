package com.atoma.marketplace.merchant;

import com.atoma.marketplace.auth.dto.AuthDtos;
import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.common.enums.BusinessType;
import com.atoma.marketplace.common.enums.UserRole;
import com.atoma.marketplace.common.enums.UserStatus;
import com.atoma.marketplace.common.enums.OtpChannel;
import com.atoma.marketplace.common.enums.PayoutMethod;
import com.atoma.marketplace.common.enums.RepresentativeRole;
import com.atoma.marketplace.merchant.dto.OnboardingDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantOnboardingV2IntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void merchantV2OnboardingWizardAndApproval() throws Exception {
        var phone = "+9370" + String.format("%07d", Math.abs(UUID.randomUUID().hashCode() % 10_000_000));
        var sendOtp = new AuthDtos.OtpSendRequest(OtpChannel.PHONE, phone);
        mockMvc.perform(post("/api/v1/auth/otp/send")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sendOtp)))
                .andExpect(status().isOk());

        var verify = new AuthDtos.OtpVerifyRequest(OtpChannel.PHONE, phone, "000000");
        var authResponse = mockMvc.perform(post("/api/v1/auth/otp/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verify)))
                .andExpect(status().isOk())
                .andReturn();
        var token = objectMapper.readTree(authResponse.getResponse().getContentAsString()).get("accessToken").asText();

        saveStep(token, 1, new OnboardingDtos.WizardStepRequest(
                1,
                new OnboardingDtos.BusinessStepRequest(
                        BusinessType.REGISTERED_SHOP, "Herat Saffron House", "Food & grocery"),
                null, null, null, null, null));

        saveStep(token, 2, new OnboardingDtos.WizardStepRequest(
                2, null,
                new OnboardingDtos.OwnerStepRequest(RepresentativeRole.OWNER, "Ahmad Rahimi", null),
                null, null, null, null));

        saveStep(token, 3, new OnboardingDtos.WizardStepRequest(
                3, null, null,
                new OnboardingDtos.DocumentsStepRequest(List.of(
                        new OnboardingDtos.DocumentItemRequest(
                                "BUSINESS_LICENSE", "https://files.example/licence.pdf",
                                "KBL-TR-2024-118734", LocalDate.of(2026, 10, 30)),
                        new OnboardingDtos.DocumentItemRequest(
                                "OWNER_TAZKIRA", "https://files.example/tazkira.pdf",
                                "1402-0101-38472", LocalDate.of(2026, 10, 29)),
                        new OnboardingDtos.DocumentItemRequest(
                                "TIN", "https://files.example/tin.pdf", "1000123456", null)
                )),
                null, null, null));

        saveStep(token, 4, new OnboardingDtos.WizardStepRequest(
                4, null, null, null,
                new OnboardingDtos.StoreAddressStepRequest(
                        "xmnsc", "cxc", "Khair Khana", "Kabul", "asc",
                        new BigDecimal("34.5361"), new BigDecimal("69.1790")),
                null, null));

        saveStep(token, 5, new OnboardingDtos.WizardStepRequest(
                5, null, null, null, null,
                new OnboardingDtos.PayoutStepRequest(PayoutMethod.ATOMA_WALLET, null),
                null));

        saveStep(token, 6, new OnboardingDtos.WizardStepRequest(
                6, null, null, null, null, null,
                new OnboardingDtos.TermsStepRequest(List.of(
                        new OnboardingDtos.ConsentItemRequest("MERCHANT_AGREEMENT", "3.2", true),
                        new OnboardingDtos.ConsentItemRequest("DATA_ACCURACY", "1.0", true),
                        new OnboardingDtos.ConsentItemRequest("VERIFICATION_CONSENT", "1.0", true)
                ))));

        mockMvc.perform(post("/api/v1/merchant/onboarding/application/submit")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.applicationNumber").exists())
                .andExpect(jsonPath("$.workflowStatus").value("UNDER_REVIEW"));

        var statusResponse = mockMvc.perform(get("/api/v1/merchant/onboarding/application/status")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.checks", hasSize(4)))
                .andReturn();

        var merchantId = objectMapper.readTree(statusResponse.getResponse().getContentAsString())
                .get("merchantId").asText();

        var adminToken = seedAdminAndLogin();

        mockMvc.perform(put("/api/v1/admin/merchants/" + merchantId + "/application/review")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OnboardingDtos.AdminApplicationReviewRequest(
                                OnboardingDtos.AdminReviewAction.APPROVE,
                                null, null, null, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.workflowStatus").value("APPROVED"));
    }

    private void saveStep(String token, int step, OnboardingDtos.WizardStepRequest request) throws Exception {
        mockMvc.perform(put("/api/v1/merchant/onboarding/wizard/step")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.currentWizardStep").value(step));
    }

    private String seedAdminAndLogin() throws Exception {
        var email = "admin-v2-" + UUID.randomUUID() + "@test.com";
        var phone = "+97799" + String.format("%07d", Math.abs(email.hashCode() % 10_000_000));
        userRepository.save(User.builder()
                .email(email)
                .phone(phone)
                .passwordHash(passwordEncoder.encode("Password1!"))
                .firstName("Admin")
                .lastName("User")
                .status(UserStatus.ACTIVE)
                .roles(Set.of(UserRole.ADMIN))
                .build());
        return loginWithPassword(email);
    }

    private String loginWithPassword(String email) throws Exception {
        var login = new AuthDtos.LoginRequest(email, "Password1!");
        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andReturn();
        if (loginResponse.getResponse().getStatus() != 200) {
            throw new IllegalStateException("Admin login failed for tests — seed admin user");
        }
        return objectMapper.readTree(loginResponse.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
