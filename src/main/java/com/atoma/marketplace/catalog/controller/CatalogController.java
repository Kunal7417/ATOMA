package com.atoma.marketplace.catalog.controller;

import com.atoma.marketplace.common.dto.PageResponse;
import com.atoma.marketplace.product.dto.ProductDtos;
import com.atoma.marketplace.product.service.ProductService;
import com.atoma.marketplace.search.service.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/catalog")
@RequiredArgsConstructor
@Tag(name = "Catalog", description = "Public catalog endpoints for customer website")
public class CatalogController {

    private final SearchService searchService;
    private final ProductService productService;

    @GetMapping("/products")
    @Operation(summary = "Browse active products")
    public PageResponse<ProductDtos.ProductResponse> browse(@PageableDefault(size = 20) Pageable pageable) {
        return PageResponse.from(productService.listActiveProducts(pageable));
    }

    @GetMapping("/products/search")
    @Operation(summary = "Search products")
    public PageResponse<ProductDtos.ProductResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) UUID categoryId,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        return PageResponse.from(searchService.searchProducts(q, categoryId, pageable));
    }

    @GetMapping("/products/{productId}")
    @Operation(summary = "Product detail page")
    public ProductDtos.ProductResponse productDetail(@PathVariable UUID productId) {
        return productService.getProduct(productId);
    }

    @GetMapping("/categories")
    @Operation(summary = "List product categories")
    public List<ProductDtos.CategoryResponse> categories() {
        return productService.listCategories();
    }
}
