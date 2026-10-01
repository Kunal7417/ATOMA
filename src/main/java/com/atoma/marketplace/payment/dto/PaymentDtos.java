package com.atoma.marketplace.payment.dto;

import com.atoma.marketplace.common.enums.PaymentStatus;
import com.atoma.marketplace.common.enums.SettlementStatus;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.UUID;

public class PaymentDtos {

    @Value
    @Builder
    public static class PaymentResponse {
        UUID id;
        UUID orderId;
        String transactionRef;
        String walletTransactionId;
        PaymentStatus status;
        BigDecimal amount;
        boolean escrowHeld;
        int retryAttempts;
        String failureReason;
    }

    @Value
    @Builder
    public static class SettlementResponse {
        UUID id;
        UUID merchantId;
        UUID orderId;
        BigDecimal grossAmount;
        BigDecimal platformCommission;
        BigDecimal merchantNetAmount;
        SettlementStatus status;
        String walletSettlementRef;
    }
}
