package com.atoma.marketplace.merchant.controller;

import com.atoma.marketplace.auth.security.SecurityUtils;
import com.atoma.marketplace.common.dto.PageResponse;
import com.atoma.marketplace.common.enums.ProductStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.merchant.dto.MerchantDtos;
import com.atoma.marketplace.merchant.service.MerchantService;
import com.atoma.marketplace.notification.entity.Notification;
import com.atoma.marketplace.notification.service.NotificationService;
import com.atoma.marketplace.payment.dto.PaymentDtos;
import com.atoma.marketplace.payment.service.PaymentService;
import com.atoma.marketplace.analytics.dto.AnalyticsDtos;
import com.atoma.marketplace.analytics.service.AnalyticsService;
import com.atoma.marketplace.order.dto.OrderDtos;
import com.atoma.marketplace.order.service.OrderService;
import com.atoma.marketplace.product.dto.ProductDtos;
import com.atoma.marketplace.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/merchant")
@RequiredArgsConstructor
@Tag(name = "Merchant", description = "Merchant Mobile App and Web Portal APIs")
public class MerchantController {

    private final MerchantService merchantService;
    private final ProductService productService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final AnalyticsService analyticsService;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @PostMapping("/onboard")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Onboard merchant business profile")
    public MerchantDtos.MerchantResponse onboard(@Valid @RequestBody MerchantDtos.MerchantOnboardRequest request) {
        return merchantService.onboard(request);
    }

    @PostMapping("/kyc/documents")
    @Operation(summary = "Upload KYC/KYB document")
    public MerchantDtos.MerchantResponse uploadKyc(@Valid @RequestBody MerchantDtos.KycDocumentRequest request) {
        return merchantService.uploadKycDocument(request);
    }

    @GetMapping("/kyc/documents")
    @Operation(summary = "List uploaded KYC/KYB documents")
    public java.util.List<MerchantDtos.KycDocumentResponse> listKycDocuments() {
        return merchantService.listKycDocuments();
    }

    @GetMapping("/profile")
    @Operation(summary = "Get current merchant profile")
    public MerchantDtos.MerchantResponse profile() {
        return merchantService.getMyMerchantProfile();
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create product listing")
    public ProductDtos.ProductResponse createProduct(@Valid @RequestBody ProductDtos.CreateProductRequest request) {
        return productService.createProduct(request);
    }

    @PutMapping("/products/{productId}")
    @Operation(summary = "Update product listing")
    public ProductDtos.ProductResponse updateProduct(
            @PathVariable UUID productId,
            @Valid @RequestBody ProductDtos.UpdateProductRequest request
    ) {
        return productService.updateProduct(productId, request);
    }

    @PutMapping("/products/{productId}/deactivate")
    @Operation(summary = "Deactivate product listing")
    public ProductDtos.ProductResponse deactivateProduct(@PathVariable UUID productId) {
        return productService.deactivateProduct(productId);
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Get merchant product by id")
    public ProductDtos.ProductResponse getProduct(@PathVariable UUID productId) {
        return productService.getMerchantProduct(productId);
    }

    @GetMapping("/products")
    @Operation(summary = "List merchant products")
    public PageResponse<ProductDtos.ProductResponse> listProducts(
            @RequestParam(required = false) ProductStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(productService.listMerchantProducts(status, pageable));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(summary = "Get merchant order details")
    public OrderDtos.OrderResponse getOrder(@PathVariable UUID orderId) {
        return orderService.getMerchantOrder(orderId);
    }

    @GetMapping("/orders")
    @Operation(summary = "List merchant orders")
    public PageResponse<OrderDtos.OrderResponse> listOrders(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(orderService.getMerchantOrders(pageable));
    }

    @PutMapping("/orders/{orderId}/status")
    @Operation(summary = "Update order fulfillment status")
    public OrderDtos.OrderResponse updateOrderStatus(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderDtos.UpdateOrderStatusRequest request
    ) {
        return orderService.updateOrderStatus(orderId, request);
    }

    @GetMapping("/settlements")
    @Operation(summary = "List merchant settlements")
    public PageResponse<PaymentDtos.SettlementResponse> settlements(@PageableDefault(size = 20) Pageable pageable) {
        var merchant = merchantService.getOwnedMerchant();
        return PageResponse.from(paymentService.getMerchantSettlements(merchant.getId(), pageable));
    }

    @GetMapping("/notifications")
    @Operation(summary = "In-app notifications for merchant account")
    public PageResponse<Notification> notifications(@PageableDefault(size = 20) Pageable pageable) {
        var userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw MarketplaceException.unauthorized("Authentication required");
        }
        return PageResponse.from(notificationService.getUserNotifications(userId, pageable));
    }

    @GetMapping("/analytics/dashboard")
    @Operation(summary = "Merchant analytics dashboard")
    public AnalyticsDtos.MerchantDashboardResponse dashboard() {
        return analyticsService.getMerchantDashboard();
    }
}
