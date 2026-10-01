package com.atoma.marketplace.payment.service;

import com.atoma.marketplace.common.enums.OrderStatus;
import com.atoma.marketplace.common.enums.PaymentStatus;
import com.atoma.marketplace.common.enums.SettlementStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.notification.service.NotificationService;
import com.atoma.marketplace.order.entity.Order;
import com.atoma.marketplace.order.repository.OrderRepository;
import com.atoma.marketplace.payment.dto.PaymentDtos;
import com.atoma.marketplace.payment.entity.Payment;
import com.atoma.marketplace.payment.entity.Settlement;
import com.atoma.marketplace.payment.integration.AtomaWalletClient;
import com.atoma.marketplace.payment.repository.PaymentRepository;
import com.atoma.marketplace.payment.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int MAX_PAYMENT_RETRIES = 3;

    private final PaymentRepository paymentRepository;
    private final SettlementRepository settlementRepository;
    private final OrderRepository orderRepository;
    private final AtomaWalletClient walletClient;
    private final NotificationService notificationService;

    @Transactional
    public PaymentDtos.PaymentResponse processPayment(UUID orderId) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> MarketplaceException.notFound("Order", orderId));

        if (order.getStatus() != OrderStatus.PAYMENT_PENDING && order.getStatus() != OrderStatus.PAYMENT_FAILED) {
            throw MarketplaceException.badRequest("Order is not eligible for payment");
        }

        var payment = paymentRepository.findByOrderId(orderId).orElseGet(() ->
                Payment.builder()
                        .order(order)
                        .transactionRef("TXN-" + UUID.randomUUID())
                        .status(PaymentStatus.PENDING)
                        .amount(order.getTotalAmount())
                        .escrowHeld(true)
                        .build());

        var walletResult = walletClient.chargeWallet(order.getCustomer().getId(), order.getTotalAmount(), payment.getTransactionRef());

        if (walletResult.success()) {
            payment.setStatus(PaymentStatus.HELD);
            payment.setWalletTransactionId(walletResult.walletTransactionId());
            order.setStatus(OrderStatus.CONFIRMED);
            notificationService.notifyPaymentConfirmed(order);
            notificationService.notifyMerchantNewOrder(order);
            createSettlement(order, payment);
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setFailureReason(walletResult.failureReason());
            payment.setRetryAttempts(payment.getRetryAttempts() + 1);
            order.setPaymentRetryCount(order.getPaymentRetryCount() + 1);
            order.setStatus(order.getPaymentRetryCount() >= MAX_PAYMENT_RETRIES
                    ? OrderStatus.CANCELLED
                    : OrderStatus.PAYMENT_FAILED);
            notificationService.notifyPaymentFailed(order);
        }

        orderRepository.save(order);
        return toPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional
    public PaymentDtos.PaymentResponse retryPayment(UUID orderId) {
        var order = getOrder(orderId);
        if (order.getPaymentRetryCount() >= MAX_PAYMENT_RETRIES) {
            throw MarketplaceException.badRequest("Maximum payment retry attempts exceeded");
        }
        return processPayment(orderId);
    }

    @Transactional
    public PaymentDtos.PaymentResponse releaseEscrow(UUID orderId) {
        var payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> MarketplaceException.notFound("Payment for order", orderId));
        if (payment.getStatus() != PaymentStatus.HELD) {
            throw MarketplaceException.badRequest("Payment is not in escrow hold state");
        }

        payment.setStatus(PaymentStatus.SETTLED);
        payment.setEscrowHeld(false);
        walletClient.releaseEscrow(payment.getWalletTransactionId());

        settlementRepository.findByOrderId(orderId).ifPresent(settlement -> {
            settlement.setStatus(SettlementStatus.COMPLETED);
            settlementRepository.save(settlement);
            notificationService.notifySettlementCompleted(settlement);
        });

        return toPaymentResponse(paymentRepository.save(payment));
    }

    @Transactional(readOnly = true)
    public Page<PaymentDtos.SettlementResponse> getMerchantSettlements(UUID merchantId, Pageable pageable) {
        return settlementRepository.findByMerchantId(merchantId, pageable).map(this::toSettlementResponse);
    }

    private void createSettlement(Order order, Payment payment) {
        var commissionRate = order.getMerchant().getCommissionRate()
                .divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
        var commission = order.getTotalAmount().multiply(commissionRate).setScale(2, RoundingMode.HALF_UP);
        var merchantNet = order.getTotalAmount().subtract(commission);

        settlementRepository.save(Settlement.builder()
                .merchant(order.getMerchant())
                .order(order)
                .grossAmount(order.getTotalAmount())
                .platformCommission(commission)
                .merchantNetAmount(merchantNet)
                .status(SettlementStatus.PENDING)
                .walletSettlementRef("SET-" + payment.getTransactionRef())
                .build());
    }

    private Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> MarketplaceException.notFound("Order", orderId));
    }

    private PaymentDtos.PaymentResponse toPaymentResponse(Payment payment) {
        return PaymentDtos.PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder().getId())
                .transactionRef(payment.getTransactionRef())
                .walletTransactionId(payment.getWalletTransactionId())
                .status(payment.getStatus())
                .amount(payment.getAmount())
                .escrowHeld(payment.isEscrowHeld())
                .retryAttempts(payment.getRetryAttempts())
                .failureReason(payment.getFailureReason())
                .build();
    }

    private PaymentDtos.SettlementResponse toSettlementResponse(Settlement settlement) {
        return PaymentDtos.SettlementResponse.builder()
                .id(settlement.getId())
                .merchantId(settlement.getMerchant().getId())
                .orderId(settlement.getOrder().getId())
                .grossAmount(settlement.getGrossAmount())
                .platformCommission(settlement.getPlatformCommission())
                .merchantNetAmount(settlement.getMerchantNetAmount())
                .status(settlement.getStatus())
                .walletSettlementRef(settlement.getWalletSettlementRef())
                .build();
    }
}
