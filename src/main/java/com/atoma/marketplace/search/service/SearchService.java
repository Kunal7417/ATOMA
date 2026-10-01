package com.atoma.marketplace.search.service;

import com.atoma.marketplace.common.enums.ProductStatus;
import com.atoma.marketplace.product.dto.ProductDtos;
import com.atoma.marketplace.product.repository.ProductRepository;
import com.atoma.marketplace.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SearchService {

    private final ProductRepository productRepository;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public Page<ProductDtos.ProductResponse> searchProducts(String query, UUID categoryId, Pageable pageable) {
        if (categoryId != null) {
            return productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable)
                    .map(productService::toPublicResponse);
        }
        if (query == null || query.isBlank()) {
            return productService.listActiveProducts(pageable);
        }
        return productRepository.searchActiveProducts(query.trim(), ProductStatus.ACTIVE, pageable)
                .map(productService::toPublicResponse);
    }
}
