package com.atoma.marketplace.order.service;

import com.atoma.marketplace.auth.repository.UserRepository;
import com.atoma.marketplace.auth.security.SecurityUtils;
import com.atoma.marketplace.common.enums.DisputeStatus;
import com.atoma.marketplace.common.enums.OrderStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.compliance.entity.Dispute;
import com.atoma.marketplace.compliance.repository.DisputeRepository;
import com.atoma.marketplace.merchant.service.MerchantService;
import com.atoma.marketplace.notification.service.NotificationService;
import com.atoma.marketplace.order.dto.OrderDtos;
import com.atoma.marketplace.order.entity.CartItem;
import com.atoma.marketplace.order.entity.Order;
import com.atoma.marketplace.order.entity.OrderItem;
import com.atoma.marketplace.order.entity.WishlistItem;
import com.atoma.marketplace.order.repository.CartItemRepository;
import com.atoma.marketplace.order.repository.OrderRepository;
import com.atoma.marketplace.order.repository.WishlistItemRepository;
import com.atoma.marketplace.product.entity.Product;
import com.atoma.marketplace.product.repository.ProductRepository;
import com.atoma.marketplace.product.service.ProductService;
import com.atoma.marketplace.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private static final BigDecimal DELIVERY_FEE = new BigDecimal("50.00");
    private static final BigDecimal TAX_RATE = new BigDecimal("0.10");

    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final OrderRepository orderRepository;
    private final DisputeRepository disputeRepository;
    private final ProductRepository productRepository;
    private final ProductService productService;
    private final MerchantService merchantService;
    private final UserRepository userRepository;
    private final SecurityUtils securityUtils;
    private final PaymentService paymentService;
    private final NotificationService notificationService;

    @Transactional
    public OrderDtos.CartItemResponse addToCart(OrderDtos.CartItemRequest request) {
        var customer = getCurrentUser();
        var product = productService.getActiveProduct(request.productId());

        var cartItem = cartItemRepository.findByCustomerIdAndProductId(customer.getId(), product.getId())
                .orElse(CartItem.builder()
                        .customer(customer)
                        .product(product)
                        .quantity(0)
                        .build());

        cartItem.setQuantity(cartItem.getQuantity() + request.quantity());
        cartItem.setVariantLabel(request.variantLabel());
        return toCartResponse(cartItemRepository.save(cartItem));
    }

    @Transactional(readOnly = true)
    public List<OrderDtos.CartItemResponse> getCart() {
        return cartItemRepository.findByCustomerId(requireUserId()).stream()
                .map(this::toCartResponse)
                .toList();
    }

    @Transactional
    public void removeFromCart(UUID cartItemId) {
        var cartItem = cartItemRepository.findById(cartItemId)
                .orElseThrow(() -> MarketplaceException.notFound("Cart item", cartItemId));
        if (!cartItem.getCustomer().getId().equals(requireUserId())) {
            throw MarketplaceException.forbidden("Not authorized");
        }
        cartItemRepository.delete(cartItem);
    }

    @Transactional
    public OrderDtos.OrderResponse checkout(OrderDtos.CheckoutRequest request) {
        var customer = getCurrentUser();
        var cartItems = cartItemRepository.findByCustomerId(customer.getId());
        if (cartItems.isEmpty()) {
            throw MarketplaceException.badRequest("Cart is empty");
        }

        var merchantId = cartItems.get(0).getProduct().getMerchant().getId();
        for (var item : cartItems) {
            if (!item.getProduct().getMerchant().getId().equals(merchantId)) {
                throw MarketplaceException.badRequest("Multi-merchant checkout is not supported in this release");
            }
            if (item.getQuantity() > item.getProduct().getStockQuantity()) {
                throw MarketplaceException.badRequest("Insufficient stock for " + item.getProduct().getTitle());
            }
        }

        var subtotal = cartItems.stream()
                .map(i -> i.getProduct().getPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        var taxAmount = subtotal.multiply(TAX_RATE);
        var total = subtotal.add(taxAmount).add(DELIVERY_FEE);

        var order = Order.builder()
                .orderNumber(generateOrderNumber())
                .customer(customer)
                .merchant(merchantService.getById(merchantId))
                .status(OrderStatus.PAYMENT_PENDING)
                .subtotal(subtotal)
                .taxAmount(taxAmount)
                .deliveryFee(DELIVERY_FEE)
                .discountAmount(BigDecimal.ZERO)
                .totalAmount(total)
                .couponCode(request.couponCode())
                .deliveryAddress(request.deliveryAddress())
                .items(new ArrayList<>())
                .build();

        for (var cartItem : cartItems) {
            var lineTotal = cartItem.getProduct().getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));
            order.getItems().add(OrderItem.builder()
                    .order(order)
                    .product(cartItem.getProduct())
                    .quantity(cartItem.getQuantity())
                    .unitPrice(cartItem.getProduct().getPrice())
                    .lineTotal(lineTotal)
                    .variantLabel(cartItem.getVariantLabel())
                    .build());

            var product = cartItem.getProduct();
            product.setStockQuantity(product.getStockQuantity() - cartItem.getQuantity());
            productRepository.save(product);
        }

        cartItemRepository.deleteByCustomerId(customer.getId());
        return toOrderResponse(orderRepository.save(order));
    }

    @Transactional
    public OrderDtos.OrderResponse updateOrderStatus(UUID orderId, OrderDtos.UpdateOrderStatusRequest request) {
        var order = getOrderForMerchant(orderId);
        var current = order.getStatus();
        var target = request.status();

        OrderStatusTransition.validateMerchantTransition(current, target);

        if (OrderStatusTransition.requiresTracking(target)) {
            if (request.trackingNumber() == null || request.trackingNumber().isBlank()) {
                throw MarketplaceException.badRequest("Tracking number is required when marking order as shipped");
            }
            if (request.courierName() == null || request.courierName().isBlank()) {
                throw MarketplaceException.badRequest("Courier name is required when marking order as shipped");
            }
        }

        order.setStatus(target);
        if (request.trackingNumber() != null) {
            order.setTrackingNumber(request.trackingNumber());
        }
        if (request.courierName() != null) {
            order.setCourierName(request.courierName());
        }

        var saved = orderRepository.save(order);

        if (target == OrderStatus.SHIPPED) {
            notificationService.notifyOrderShipped(saved);
        }
        if (OrderStatusTransition.isDeliveryComplete(target)) {
            paymentService.releaseEscrow(orderId);
            notificationService.notifyOrderDelivered(saved);
        }

        return toOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public OrderDtos.OrderResponse getMerchantOrder(UUID orderId) {
        return toOrderResponse(getOrderForMerchant(orderId));
    }

    @Transactional(readOnly = true)
    public Page<OrderDtos.OrderResponse> getCustomerOrders(Pageable pageable) {
        return orderRepository.findByCustomerId(requireUserId(), pageable).map(this::toOrderResponse);
    }

    @Transactional(readOnly = true)
    public Page<OrderDtos.OrderResponse> getMerchantOrders(Pageable pageable) {
        var merchant = merchantService.getOwnedMerchant();
        return orderRepository.findByMerchantId(merchant.getId(), pageable).map(this::toOrderResponse);
    }

    @Transactional
    public void addToWishlist(UUID productId) {
        var customer = getCurrentUser();
        if (wishlistItemRepository.existsByCustomerIdAndProductId(customer.getId(), productId)) {
            return;
        }
        var product = productService.getActiveProduct(productId);
        wishlistItemRepository.save(WishlistItem.builder().customer(customer).product(product).build());
        product.setFavoriteCount(product.getFavoriteCount() + 1);
        productRepository.save(product);
    }

    @Transactional
    public void removeFromWishlist(UUID productId) {
        var customerId = requireUserId();
        wishlistItemRepository.findByCustomerIdAndProductId(customerId, productId)
                .ifPresent(item -> {
                    wishlistItemRepository.delete(item);
                    var product = item.getProduct();
                    product.setFavoriteCount(Math.max(0, product.getFavoriteCount() - 1));
                    productRepository.save(product);
                });
    }

    @Transactional(readOnly = true)
    public List<Product> getWishlist() {
        return wishlistItemRepository.findByCustomerId(requireUserId()).stream()
                .map(WishlistItem::getProduct)
                .toList();
    }

    @Transactional
    public Dispute requestRefund(UUID orderId, OrderDtos.RefundRequest request) {
        var order = orderRepository.findById(orderId)
                .orElseThrow(() -> MarketplaceException.notFound("Order", orderId));
        if (!order.getCustomer().getId().equals(requireUserId())) {
            throw MarketplaceException.forbidden("Not authorized");
        }
        if (order.getStatus() != OrderStatus.DELIVERED) {
            throw MarketplaceException.badRequest("Refund can only be requested for delivered orders");
        }

        order.setStatus(OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        return disputeRepository.save(Dispute.builder()
                .order(order)
                .customer(order.getCustomer())
                .merchant(order.getMerchant())
                .status(DisputeStatus.OPEN)
                .reason(request.reason())
                .description(request.description())
                .slaDueAt(Instant.now().plus(48, ChronoUnit.HOURS))
                .build());
    }

    public Order getOrder(UUID orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> MarketplaceException.notFound("Order", orderId));
    }

    private Order getOrderForMerchant(UUID orderId) {
        var merchant = merchantService.getOwnedMerchant();
        var order = getOrder(orderId);
        if (!order.getMerchant().getId().equals(merchant.getId())) {
            throw MarketplaceException.forbidden("Not authorized to manage this order");
        }
        return order;
    }

    private com.atoma.marketplace.auth.entity.User getCurrentUser() {
        var userId = requireUserId();
        return userRepository.findById(userId)
                .orElseThrow(() -> MarketplaceException.notFound("User", userId));
    }

    private UUID requireUserId() {
        var userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw MarketplaceException.unauthorized("Authentication required");
        }
        return userId;
    }

    private String generateOrderNumber() {
        return "ORD-" + System.currentTimeMillis();
    }

    private OrderDtos.CartItemResponse toCartResponse(CartItem item) {
        var lineTotal = item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        return OrderDtos.CartItemResponse.builder()
                .id(item.getId())
                .productId(item.getProduct().getId())
                .productTitle(item.getProduct().getTitle())
                .unitPrice(item.getProduct().getPrice())
                .quantity(item.getQuantity())
                .lineTotal(lineTotal)
                .build();
    }

    public OrderDtos.OrderResponse toOrderResponse(Order order) {
        var items = order.getItems().stream()
                .map(i -> OrderDtos.OrderItemResponse.builder()
                        .productId(i.getProduct().getId())
                        .productTitle(i.getProduct().getTitle())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .lineTotal(i.getLineTotal())
                        .build())
                .toList();

        return OrderDtos.OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .subtotal(order.getSubtotal())
                .taxAmount(order.getTaxAmount())
                .deliveryFee(order.getDeliveryFee())
                .discountAmount(order.getDiscountAmount())
                .totalAmount(order.getTotalAmount())
                .deliveryAddress(order.getDeliveryAddress())
                .trackingNumber(order.getTrackingNumber())
                .courierName(order.getCourierName())
                .items(items)
                .build();
    }
}
