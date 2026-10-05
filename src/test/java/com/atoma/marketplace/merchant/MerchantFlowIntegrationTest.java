package com.atoma.marketplace.merchant;

import com.atoma.marketplace.auth.entity.User;
import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.dto.AuthDtos;
import com.atoma.marketplace.common.enums.KycDocumentType;
import com.atoma.marketplace.common.enums.OrderStatus;
import com.atoma.marketplace.common.enums.UserStatus;
import com.atoma.marketplace.common.enums.UserRole;
import com.atoma.marketplace.merchant.dto.MerchantDtos;
import com.atoma.marketplace.order.dto.OrderDtos;
import com.atoma.marketplace.product.dto.ProductDtos;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MerchantFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private String merchantToken;
    private String adminToken;
    private String customerToken;
    private UUID categoryId;

    @BeforeEach
    void setUp() throws Exception {
        merchantToken = registerAndLogin("merchant-" + UUID.randomUUID() + "@test.com", UserRole.MERCHANT);
        adminToken = seedAdminAndLogin();
        customerToken = registerAndLogin("customer-" + UUID.randomUUID() + "@test.com", UserRole.CUSTOMER);
        categoryId = createCategory();
    }

    private String seedAdminAndLogin() throws Exception {
        var email = "admin-" + UUID.randomUUID() + "@test.com";
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
        var login = new AuthDtos.LoginRequest(email, "Password1!");
        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResponse.getResponse().getContentAsString()).get("accessToken").asText();
    }

    @Test
    void merchantEndToEndFlow() throws Exception {
        mockMvc.perform(post("/api/v1/merchant/onboard")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new MerchantDtos.MerchantOnboardRequest(
                                "Test Shop",
                                "Groceries",
                                "TIN-123",
                                "LIC-456",
                                new BigDecimal("27.7172"),
                                new BigDecimal("85.3240"),
                                "123 Main St",
                                "Kathmandu"
                        ))))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/merchant/products")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProduct())))
                .andExpect(status().isForbidden());

        for (KycDocumentType type : KycDocumentType.REQUIRED_FOR_SUBMISSION) {
            mockMvc.perform(post("/api/v1/merchant/kyc/documents")
                            .header("Authorization", "Bearer " + merchantToken)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(new MerchantDtos.KycDocumentRequest(
                                    type.name(),
                                    "https://files.example/" + type.name().toLowerCase() + ".pdf",
                                    "DOC-" + type.name(),
                                    java.time.LocalDate.of(2027, 3, 2)
                            ))))
                    .andExpect(status().isOk());
        }

        var merchantId = extractMerchantId();

        mockMvc.perform(put("/api/v1/admin/merchants/" + merchantId + "/verify")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VERIFIED"));

        var productResponse = mockMvc.perform(post("/api/v1/merchant/products")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sampleProduct())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING_APPROVAL"))
                .andReturn();

        var productId = objectMapper.readTree(productResponse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(put("/api/v1/admin/products/" + productId + "/approve")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("approved", "true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(post("/api/v1/customer/cart/items")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderDtos.CartItemRequest(
                                UUID.fromString(productId), 1, null))))
                .andExpect(status().isOk());

        var orderResponse = mockMvc.perform(post("/api/v1/customer/checkout")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderDtos.CheckoutRequest("Customer Addr", null))))
                .andExpect(status().isOk())
                .andReturn();

        var orderId = objectMapper.readTree(orderResponse.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/v1/customer/orders/" + orderId + "/pay")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk());

        mockMvc.perform(put("/api/v1/merchant/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderDtos.UpdateOrderStatusRequest(
                                OrderStatus.SHIPPED, "TRACK-1", "DHL"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SHIPPED"));

        mockMvc.perform(put("/api/v1/merchant/orders/" + orderId + "/status")
                        .header("Authorization", "Bearer " + merchantToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderDtos.UpdateOrderStatusRequest(
                                OrderStatus.DELIVERED, null, null))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DELIVERED"));

        mockMvc.perform(get("/api/v1/merchant/settlements")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("COMPLETED"));

        mockMvc.perform(get("/api/v1/merchant/analytics/dashboard")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalOrders").value(greaterThanOrEqualTo(1)));
    }

    private UUID extractMerchantId() throws Exception {
        var profile = mockMvc.perform(get("/api/v1/merchant/profile")
                        .header("Authorization", "Bearer " + merchantToken))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(profile.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID createCategory() throws Exception {
        var response = mockMvc.perform(post("/api/v1/admin/categories")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ProductDtos.CategoryRequest(
                                "Groceries-" + UUID.randomUUID().toString().substring(0, 6),
                                "Food and grocery",
                                null,
                                1,
                                new BigDecimal("5.00")))))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(response.getResponse().getContentAsString()).get("id").asText());
    }

    private ProductDtos.CreateProductRequest sampleProduct() {
        return new ProductDtos.CreateProductRequest(
                categoryId,
                "Rice 5kg",
                "Premium rice",
                "SKU-" + UUID.randomUUID().toString().substring(0, 8),
                new BigDecimal("500.00"),
                new BigDecimal("550.00"),
                100,
                "Local",
                java.util.List.of("https://cdn.example/rice.png")
        );
    }

    private String registerAndLogin(String email, UserRole role) throws Exception {
        var register = new AuthDtos.RegisterRequest(
                email,
                "+9779800" + String.format("%06d", Math.abs(email.hashCode() % 1_000_000)),
                "Password1!",
                "Test",
                "User",
                Set.of(role)
        );
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(register)))
                .andExpect(status().isCreated());

        var login = new AuthDtos.LoginRequest(email, "Password1!");
        var loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResponse.getResponse().getContentAsString()).get("accessToken").asText();
    }
}
