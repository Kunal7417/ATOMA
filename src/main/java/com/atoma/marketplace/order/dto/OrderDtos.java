package com.atoma.marketplace.order.dto;

import com.atoma.marketplace.common.enums.OrderStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class OrderDtos {

    public record CartItemRequest(
            @NotNull UUID productId,
            @NotNull @Min(1) Integer quantity,
            String variantLabel
    ) {}

    public record CheckoutRequest(
            @NotBlank String deliveryAddress,
            String couponCode
    ) {}

    public record UpdateOrderStatusRequest(
            @NotNull OrderStatus status,
            String trackingNumber,
            String courierName
    ) {}

    public record RefundRequest(
            @NotBlank String reason,
            String description
    ) {}

    @Value
    @Builder
    public static class CartItemResponse {
        UUID id;
        UUID productId;
        String productTitle;
        BigDecimal unitPrice;
        int quantity;
        BigDecimal lineTotal;
    }

    @Value
    @Builder
    public static class OrderResponse {
        UUID id;
        String orderNumber;
        OrderStatus status;
        BigDecimal subtotal;
        BigDecimal taxAmount;
        BigDecimal deliveryFee;
        BigDecimal discountAmount;
        BigDecimal totalAmount;
        String deliveryAddress;
        String trackingNumber;
        String courierName;
        List<OrderItemResponse> items;
    }

    @Value
    @Builder
    public static class OrderItemResponse {
        UUID productId;
        String productTitle;
        int quantity;
        BigDecimal unitPrice;
        BigDecimal lineTotal;
    }
}
