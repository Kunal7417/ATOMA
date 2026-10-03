package com.atoma.marketplace.admin.controller;

import com.atoma.marketplace.admin.entity.PlatformPolicy;
import com.atoma.marketplace.admin.service.AdminService;
import com.atoma.marketplace.analytics.dto.AnalyticsDtos;
import com.atoma.marketplace.analytics.service.AnalyticsService;
import com.atoma.marketplace.common.dto.PageResponse;
import com.atoma.marketplace.common.enums.DisputeStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import com.atoma.marketplace.compliance.entity.AuditLog;
import com.atoma.marketplace.compliance.entity.Dispute;
import com.atoma.marketplace.merchant.dto.MerchantDtos;
import com.atoma.marketplace.merchant.dto.OnboardingDtos;
import com.atoma.marketplace.merchant.service.MerchantOnboardingService;
import com.atoma.marketplace.merchant.service.MerchantService;
import com.atoma.marketplace.product.dto.ProductDtos;
import com.atoma.marketplace.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin", description = "Admin Portal APIs")
public class AdminController {

    private final MerchantService merchantService;
    private final MerchantOnboardingService merchantOnboardingService;
    private final ProductService productService;
    private final AdminService adminService;
    private final AnalyticsService analyticsService;

    @GetMapping("/merchants")
    @Operation(summary = "List merchants by verification status")
    public PageResponse<MerchantDtos.MerchantResponse> listMerchants(
            @RequestParam MerchantStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(merchantService.listByStatus(status, pageable));
    }

    @PutMapping("/merchants/{merchantId}/verify")
    @Operation(summary = "Approve or reject merchant KYC")
    public MerchantDtos.MerchantResponse verifyMerchant(
            @PathVariable UUID merchantId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String notes
    ) {
        return merchantService.verifyMerchant(merchantId, approved, notes);
    }

    @PutMapping("/merchants/{merchantId}/application/review")
    @Operation(summary = "Merchant V2 application review (approve, reject, request updates)")
    public OnboardingDtos.ApplicationStatusResponse reviewApplication(
            @PathVariable UUID merchantId,
            @Valid @RequestBody OnboardingDtos.AdminApplicationReviewRequest request
    ) {
        return merchantOnboardingService.adminReview(merchantId, request);
    }

    @PutMapping("/merchants/{merchantId}/suspend")
    @Operation(summary = "Suspend merchant (e.g. expired licence)")
    public OnboardingDtos.ApplicationStatusResponse suspendMerchant(
            @PathVariable UUID merchantId,
            @Valid @RequestBody OnboardingDtos.SuspendMerchantRequest request
    ) {
        return merchantOnboardingService.suspendMerchant(merchantId, request);
    }

    @PutMapping("/products/{productId}/approve")
    @Operation(summary = "Approve or reject product listing")
    public ProductDtos.ProductResponse approveProduct(
            @PathVariable UUID productId,
            @RequestParam boolean approved
    ) {
        return productService.approveProduct(productId, approved);
    }

    @PostMapping("/categories")
    @Operation(summary = "Manage category taxonomy")
    public ProductDtos.CategoryResponse createCategory(@Valid @RequestBody ProductDtos.CategoryRequest request) {
        return productService.createCategory(request);
    }

    @GetMapping("/categories")
    @Operation(summary = "List all categories")
    public java.util.List<ProductDtos.CategoryResponse> listCategories() {
        return productService.listCategories();
    }

    @PostMapping("/policies")
    @Operation(summary = "Configure platform policies")
    public PlatformPolicy upsertPolicy(@Valid @RequestBody AdminService.PolicyRequest request) {
        return adminService.upsertPolicy(request);
    }

    @GetMapping("/policies")
    @Operation(summary = "List platform policies")
    public java.util.List<PlatformPolicy> listPolicies() {
        return adminService.listPolicies();
    }

    @GetMapping("/disputes")
    @Operation(summary = "List dispute queue")
    public PageResponse<Dispute> listDisputes(
            @RequestParam(required = false) DisputeStatus status,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(adminService.listDisputes(status, pageable));
    }

    @PutMapping("/disputes/{disputeId}")
    @Operation(summary = "Resolve or escalate dispute")
    public Dispute resolveDispute(
            @PathVariable UUID disputeId,
            @RequestParam DisputeStatus status,
            @RequestParam(required = false) String resolutionNotes
    ) {
        return adminService.resolveDispute(disputeId, status, resolutionNotes);
    }

    @GetMapping("/audit-logs")
    @Operation(summary = "View audit trail")
    public PageResponse<AuditLog> auditLogs(@PageableDefault(size = 50) Pageable pageable) {
        return PageResponse.from(adminService.listAuditLogs(pageable));
    }

    @GetMapping("/analytics/dashboard")
    @Operation(summary = "Platform analytics dashboard")
    public AnalyticsDtos.AdminDashboardResponse dashboard() {
        return analyticsService.getAdminDashboard();
    }
}
