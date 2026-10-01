package com.atoma.marketplace.payment.integration;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class AtomaWalletClient {

    public WalletChargeResult chargeWallet(UUID customerId, BigDecimal amount, String transactionRef) {
        // Integration stub — replace with ATOMA Pay Wallet API calls
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            return WalletChargeResult.failed("Invalid amount");
        }
        return WalletChargeResult.success("WALLET-" + transactionRef);
    }

    public void releaseEscrow(String walletTransactionId) {
        // Integration stub for escrow release after delivery confirmation
    }

    public void reversePayment(String walletTransactionId, BigDecimal amount) {
        // Integration stub for refund auto-reversal
    }

    public record WalletChargeResult(boolean success, String walletTransactionId, String failureReason) {
        public static WalletChargeResult success(String walletTransactionId) {
            return new WalletChargeResult(true, walletTransactionId, null);
        }

        public static WalletChargeResult failed(String reason) {
            return new WalletChargeResult(false, null, reason);
        }
    }
}
