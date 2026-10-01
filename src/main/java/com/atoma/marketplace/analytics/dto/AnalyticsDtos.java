package com.atoma.marketplace.analytics.dto;

import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;

public class AnalyticsDtos {

    @Value
    @Builder
    public static class AdminDashboardResponse {
        long totalOrders;
        long activeMerchants;
        long activeProducts;
        BigDecimal grossMerchandiseValue;
        BigDecimal totalCommission;
        long openDisputes;
    }

    @Value
    @Builder
    public static class MerchantDashboardResponse {
        long totalOrders;
        BigDecimal totalSales;
        BigDecimal pendingSettlements;
        long lowStockProducts;
        long openDisputes;
    }
}
