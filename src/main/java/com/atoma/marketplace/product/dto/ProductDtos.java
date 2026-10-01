package com.atoma.marketplace.product.dto;

import com.atoma.marketplace.common.enums.ProductStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Value;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class ProductDtos {

    public record CreateProductRequest(
            @NotNull UUID categoryId,
            @NotBlank String title,
            String description,
            @NotBlank String sku,
            @NotNull @DecimalMin("0.01") BigDecimal price,
            BigDecimal compareAtPrice,
            @NotNull Integer stockQuantity,
            String brand,
            List<String> imageUrls
    ) {}

    public record UpdateProductRequest(
            String title,
            String description,
            BigDecimal price,
            BigDecimal compareAtPrice,
            Integer stockQuantity,
            String brand,
            List<String> imageUrls,
            ProductStatus status
    ) {}

    @Value
    @Builder
    public static class ProductResponse {
        UUID id;
        UUID merchantId;
        String merchantName;
        UUID categoryId;
        String categoryName;
        String title;
        String slug;
        String description;
        String sku;
        BigDecimal price;
        BigDecimal compareAtPrice;
        int stockQuantity;
        int favoriteCount;
        ProductStatus status;
        String brand;
        List<String> imageUrls;
    }

    public record CategoryRequest(
            @NotBlank String name,
            String description,
            UUID parentId,
            Integer sortOrder,
            BigDecimal commissionRate
    ) {}

    @Value
    @Builder
    public static class CategoryResponse {
        UUID id;
        String name;
        String slug;
        String description;
        UUID parentId;
        boolean active;
        int sortOrder;
    }
}
