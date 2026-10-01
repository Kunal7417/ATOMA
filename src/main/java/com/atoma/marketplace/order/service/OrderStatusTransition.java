package com.atoma.marketplace.order.service;

import com.atoma.marketplace.common.enums.OrderStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

final class OrderStatusTransition {

    private static final Map<OrderStatus, Set<OrderStatus>> MERCHANT_ALLOWED = Map.of(
            OrderStatus.CONFIRMED, EnumSet.of(OrderStatus.PROCESSING, OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.PROCESSING, EnumSet.of(OrderStatus.SHIPPED, OrderStatus.CANCELLED),
            OrderStatus.SHIPPED, EnumSet.of(OrderStatus.OUT_FOR_DELIVERY, OrderStatus.DELIVERED),
            OrderStatus.OUT_FOR_DELIVERY, EnumSet.of(OrderStatus.DELIVERED)
    );

    private OrderStatusTransition() {
    }

    static void validateMerchantTransition(OrderStatus current, OrderStatus target) {
        if (current == target) {
            return;
        }
        var allowed = MERCHANT_ALLOWED.get(current);
        if (allowed == null || !allowed.contains(target)) {
            throw MarketplaceException.badRequest(
                    "Cannot change order status from " + current + " to " + target);
        }
    }

    static boolean requiresTracking(OrderStatus target) {
        return target == OrderStatus.SHIPPED || target == OrderStatus.OUT_FOR_DELIVERY;
    }

    static boolean isDeliveryComplete(OrderStatus target) {
        return target == OrderStatus.DELIVERED;
    }
}
