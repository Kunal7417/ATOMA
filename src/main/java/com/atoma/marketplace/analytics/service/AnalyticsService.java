package com.atoma.marketplace.analytics.service;

import com.atoma.marketplace.analytics.dto.AnalyticsDtos;
import com.atoma.marketplace.common.enums.DisputeStatus;
import com.atoma.marketplace.common.enums.MerchantStatus;
import com.atoma.marketplace.common.enums.OrderStatus;
import com.atoma.marketplace.common.enums.ProductStatus;
import com.atoma.marketplace.compliance.repository.DisputeRepository;
import com.atoma.marketplace.merchant.repository.MerchantRepository;
import com.atoma.marketplace.merchant.service.MerchantService;
import com.atoma.marketplace.order.repository.OrderRepository;
import com.atoma.marketplace.payment.repository.SettlementRepository;
import com.atoma.marketplace.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final MerchantRepository merchantRepository;
    private final ProductRepository productRepository;
    private final SettlementRepository settlementRepository;
    private final DisputeRepository disputeRepository;
    private final MerchantService merchantService;

    @Transactional(readOnly = true)
    public AnalyticsDtos.AdminDashboardResponse getAdminDashboard() {
        var orders = orderRepository.findAll();
        var gmv = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var commission = settlementRepository.findAll().stream()
                .map(s -> s.getPlatformCommission())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return AnalyticsDtos.AdminDashboardResponse.builder()
                .totalOrders(orders.size())
                .activeMerchants(merchantRepository.findByStatus(MerchantStatus.VERIFIED,
                        org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .activeProducts(productRepository.findByStatus(ProductStatus.ACTIVE,
                        org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .grossMerchandiseValue(gmv)
                .totalCommission(commission)
                .openDisputes(disputeRepository.findByStatus(DisputeStatus.OPEN,
                        org.springframework.data.domain.Pageable.unpaged()).getTotalElements())
                .build();
    }

    @Transactional(readOnly = true)
    public AnalyticsDtos.MerchantDashboardResponse getMerchantDashboard() {
        var merchant = merchantService.getOwnedMerchant();
        var orders = orderRepository.findByMerchantId(merchant.getId(),
                org.springframework.data.domain.Pageable.unpaged()).getContent();

        var sales = orders.stream()
                .filter(o -> o.getStatus() != OrderStatus.CANCELLED)
                .map(o -> o.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var pendingSettlements = settlementRepository.findByMerchantId(merchant.getId(),
                        org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
                .filter(s -> s.getStatus().name().equals("PENDING"))
                .map(s -> s.getMerchantNetAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        var lowStock = productRepository.findByMerchantId(merchant.getId(),
                        org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
                .filter(p -> p.getStatus() == ProductStatus.ACTIVE)
                .filter(p -> p.getStockQuantity() < 5)
                .count();

        var openDisputes = disputeRepository.findByMerchantId(merchant.getId(),
                        org.springframework.data.domain.Pageable.unpaged()).getContent().stream()
                .filter(d -> d.getStatus() == DisputeStatus.OPEN || d.getStatus() == DisputeStatus.UNDER_REVIEW)
                .count();

        return AnalyticsDtos.MerchantDashboardResponse.builder()
                .totalOrders(orders.size())
                .totalSales(sales)
                .pendingSettlements(pendingSettlements)
                .lowStockProducts(lowStock)
                .openDisputes(openDisputes)
                .build();
    }
}
