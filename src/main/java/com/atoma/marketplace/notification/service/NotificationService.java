package com.atoma.marketplace.notification.service;

import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.common.enums.NotificationChannel;
import com.atoma.marketplace.common.enums.NotificationType;
import com.atoma.marketplace.merchant.entity.Merchant;
import com.atoma.marketplace.notification.entity.Notification;
import com.atoma.marketplace.notification.repository.NotificationRepository;
import com.atoma.marketplace.order.entity.Order;
import com.atoma.marketplace.payment.entity.Settlement;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @Async
    @Transactional
    public void notifyPaymentConfirmed(Order order) {
        saveNotification(order.getCustomer().getId(), NotificationType.PAYMENT_CONFIRMED,
                "Payment confirmed", "Your payment for order " + order.getOrderNumber() + " was successful.");
    }

    @Async
    @Transactional
    public void notifyPaymentFailed(Order order) {
        saveNotification(order.getCustomer().getId(), NotificationType.PAYMENT_FAILED,
                "Payment failed", "Payment for order " + order.getOrderNumber() + " failed. Please retry.");
    }

    @Async
    @Transactional
    public void notifyMerchantNewOrder(Order order) {
        saveNotification(order.getMerchant().getOwner().getId(), NotificationType.ORDER_PLACED,
                "New order received",
                "Order " + order.getOrderNumber() + " is ready for fulfillment.");
    }

    @Async
    @Transactional
    public void notifyOrderShipped(Order order) {
        saveNotification(order.getCustomer().getId(), NotificationType.ORDER_SHIPPED,
                "Order shipped", "Your order " + order.getOrderNumber() + " has been shipped.");
    }

    @Async
    @Transactional
    public void notifyOrderDelivered(Order order) {
        saveNotification(order.getCustomer().getId(), NotificationType.DELIVERED,
                "Order delivered", "Your order " + order.getOrderNumber() + " has been delivered.");
    }

    @Async
    @Transactional
    public void notifySettlementCompleted(Settlement settlement) {
        saveNotification(settlement.getMerchant().getOwner().getId(), NotificationType.SETTLEMENT,
                "Settlement completed",
                "Settlement for order " + settlement.getOrder().getOrderNumber()
                        + " completed. Net amount: " + settlement.getMerchantNetAmount());
    }

    @Async
    @Transactional
    public void notifyKycApproved(Merchant merchant) {
        saveNotification(merchant.getOwner().getId(), NotificationType.KYC_UPDATE,
                "KYC approved", "Your merchant account has been verified. You can now list products.");
    }

    @Async
    @Transactional
    public void notifyKycRejected(Merchant merchant, String notes) {
        var detail = notes != null && !notes.isBlank() ? notes : "Please resubmit required documents.";
        saveNotification(merchant.getOwner().getId(), NotificationType.KYC_UPDATE,
                "KYC rejected", detail);
    }

    @Transactional(readOnly = true)
    public Page<Notification> getUserNotifications(UUID userId, Pageable pageable) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    private void saveNotification(UUID userId, NotificationType type, String title, String message) {
        var user = userRepository.getReferenceById(userId);
        notificationRepository.save(Notification.builder()
                .user(user)
                .type(type)
                .channel(NotificationChannel.IN_APP)
                .title(title)
                .message(message)
                .build());
    }
}
