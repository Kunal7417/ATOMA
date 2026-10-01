package com.atoma.marketplace.customer.controller;

import com.atoma.marketplace.auth.security.SecurityUtils;
import com.atoma.marketplace.common.dto.PageResponse;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.notification.entity.Notification;
import com.atoma.marketplace.notification.service.NotificationService;
import com.atoma.marketplace.order.dto.OrderDtos;
import com.atoma.marketplace.order.service.OrderService;
import com.atoma.marketplace.payment.dto.PaymentDtos;
import com.atoma.marketplace.payment.service.PaymentService;
import com.atoma.marketplace.product.dto.ProductDtos;
import com.atoma.marketplace.product.service.ProductService;
import com.atoma.marketplace.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/customer")
@RequiredArgsConstructor
@Tag(name = "Customer", description = "Customer Website and Mobile App APIs")
public class CustomerController {

    private final SearchService searchService;
    private final ProductService productService;
    private final OrderService orderService;
    private final PaymentService paymentService;
    private final NotificationService notificationService;
    private final SecurityUtils securityUtils;

    @GetMapping("/catalog/search")
    @Operation(summary = "Search and discover products")
    public PageResponse<ProductDtos.ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(searchService.searchProducts(q, categoryId, pageable));
    }

    @GetMapping("/catalog/products/{productId}")
    @Operation(summary = "Get product details")
    public ProductDtos.ProductResponse getProduct(@PathVariable UUID productId) {
        return productService.getProduct(productId);
    }

    @GetMapping("/cart")
    @Operation(summary = "Get shopping cart")
    public List<OrderDtos.CartItemResponse> getCart() {
        return orderService.getCart();
    }

    @PostMapping("/cart/items")
    @Operation(summary = "Add item to cart")
    public OrderDtos.CartItemResponse addToCart(@Valid @RequestBody OrderDtos.CartItemRequest request) {
        return orderService.addToCart(request);
    }

    @DeleteMapping("/cart/items/{cartItemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove item from cart")
    public void removeFromCart(@PathVariable UUID cartItemId) {
        orderService.removeFromCart(cartItemId);
    }

    @PostMapping("/checkout")
    @Operation(summary = "Checkout cart and create order")
    public OrderDtos.OrderResponse checkout(@Valid @RequestBody OrderDtos.CheckoutRequest request) {
        return orderService.checkout(request);
    }

    @PostMapping("/orders/{orderId}/pay")
    @Operation(summary = "Pay for order via ATOMA Pay Wallet")
    public PaymentDtos.PaymentResponse pay(@PathVariable UUID orderId) {
        return paymentService.processPayment(orderId);
    }

    @PostMapping("/orders/{orderId}/pay/retry")
    @Operation(summary = "Retry failed wallet payment")
    public PaymentDtos.PaymentResponse retryPayment(@PathVariable UUID orderId) {
        return paymentService.retryPayment(orderId);
    }

    @GetMapping("/orders")
    @Operation(summary = "Get customer order history")
    public PageResponse<OrderDtos.OrderResponse> orders(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(orderService.getCustomerOrders(pageable));
    }

    @PostMapping("/orders/{orderId}/refund")
    @Operation(summary = "Request refund for delivered order")
    public com.atoma.marketplace.compliance.entity.Dispute requestRefund(
            @PathVariable UUID orderId,
            @Valid @RequestBody OrderDtos.RefundRequest request
    ) {
        return orderService.requestRefund(orderId, request);
    }

    @PostMapping("/wishlist/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Add product to wishlist")
    public void addWishlist(@PathVariable UUID productId) {
        orderService.addToWishlist(productId);
    }

    @DeleteMapping("/wishlist/{productId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove product from wishlist")
    public void removeWishlist(@PathVariable UUID productId) {
        orderService.removeFromWishlist(productId);
    }

    @GetMapping("/notifications")
    @Operation(summary = "Get customer notifications")
    public PageResponse<Notification> notifications(@PageableDefault(size = 20) Pageable pageable) {
        var userId = securityUtils.getCurrentUserId();
        if (userId == null) {
            throw MarketplaceException.unauthorized("Authentication required");
        }
        return PageResponse.from(notificationService.getUserNotifications(userId, pageable));
    }
}
