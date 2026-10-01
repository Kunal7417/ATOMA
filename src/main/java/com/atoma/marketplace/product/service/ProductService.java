package com.atoma.marketplace.product.service;

import com.atoma.marketplace.common.enums.ProductStatus;
import com.atoma.marketplace.common.exception.MarketplaceException;
import com.atoma.marketplace.merchant.service.MerchantService;
import com.atoma.marketplace.product.dto.ProductDtos;
import com.atoma.marketplace.product.entity.Category;
import com.atoma.marketplace.product.entity.Product;
import com.atoma.marketplace.product.repository.CategoryRepository;
import com.atoma.marketplace.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final MerchantService merchantService;

    @Transactional
    public ProductDtos.ProductResponse createProduct(ProductDtos.CreateProductRequest request) {
        merchantService.assertCanManageCatalog();
        var merchant = merchantService.getOwnedMerchant();
        var category = getCategory(request.categoryId());

        var slug = generateUniqueSlug(request.title());
        var product = Product.builder()
                .merchant(merchant)
                .category(category)
                .title(request.title())
                .slug(slug)
                .description(request.description())
                .sku(request.sku())
                .price(request.price())
                .compareAtPrice(request.compareAtPrice())
                .stockQuantity(request.stockQuantity())
                .brand(request.brand())
                .imageUrls(request.imageUrls() != null ? request.imageUrls() : java.util.List.of())
                .status(ProductStatus.PENDING_APPROVAL)
                .searchVector(buildSearchVector(request.title(), request.description(), request.brand()))
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDtos.ProductResponse updateProduct(UUID productId, ProductDtos.UpdateProductRequest request) {
        merchantService.assertCanManageCatalog();
        var merchant = merchantService.getOwnedMerchant();
        var product = productRepository.findById(productId)
                .orElseThrow(() -> MarketplaceException.notFound("Product", productId));

        if (!product.getMerchant().getId().equals(merchant.getId())) {
            throw MarketplaceException.forbidden("Not authorized to update this product");
        }

        if (request.title() != null) {
            product.setTitle(request.title());
            product.setSlug(generateUniqueSlug(request.title()));
        }
        if (request.description() != null) product.setDescription(request.description());
        if (request.price() != null) product.setPrice(request.price());
        if (request.compareAtPrice() != null) product.setCompareAtPrice(request.compareAtPrice());
        if (request.stockQuantity() != null) product.setStockQuantity(request.stockQuantity());
        if (request.brand() != null) product.setBrand(request.brand());
        if (request.imageUrls() != null) product.setImageUrls(request.imageUrls());
        if (request.status() != null) {
            if (request.status() == ProductStatus.ACTIVE) {
                throw MarketplaceException.badRequest("Products require admin approval before going active");
            }
            product.setStatus(request.status());
        }
        product.setSearchVector(buildSearchVector(product.getTitle(), product.getDescription(), product.getBrand()));

        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse getProduct(UUID productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> MarketplaceException.notFound("Product", productId));
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    public Page<ProductDtos.ProductResponse> listActiveProducts(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.ACTIVE, pageable).map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public Page<ProductDtos.ProductResponse> listMerchantProducts(ProductStatus status, Pageable pageable) {
        var merchant = merchantService.getOwnedMerchant();
        Page<Product> page = status != null
                ? productRepository.findByMerchantIdAndStatus(merchant.getId(), status, pageable)
                : productRepository.findByMerchantId(merchant.getId(), pageable);
        return page.map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public ProductDtos.ProductResponse getMerchantProduct(UUID productId) {
        var merchant = merchantService.getOwnedMerchant();
        var product = productRepository.findById(productId)
                .orElseThrow(() -> MarketplaceException.notFound("Product", productId));
        if (!product.getMerchant().getId().equals(merchant.getId())) {
            throw MarketplaceException.forbidden("Not authorized to view this product");
        }
        return toResponse(product);
    }

    @Transactional
    public ProductDtos.ProductResponse deactivateProduct(UUID productId) {
        merchantService.assertCanManageCatalog();
        var merchant = merchantService.getOwnedMerchant();
        var product = productRepository.findById(productId)
                .orElseThrow(() -> MarketplaceException.notFound("Product", productId));
        if (!product.getMerchant().getId().equals(merchant.getId())) {
            throw MarketplaceException.forbidden("Not authorized to update this product");
        }
        product.setStatus(ProductStatus.INACTIVE);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDtos.ProductResponse approveProduct(UUID productId, boolean approved) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> MarketplaceException.notFound("Product", productId));
        product.setStatus(approved ? ProductStatus.ACTIVE : ProductStatus.REJECTED);
        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductDtos.CategoryResponse createCategory(ProductDtos.CategoryRequest request) {
        var slug = slugify(request.name());
        if (categoryRepository.existsBySlug(slug)) {
            throw MarketplaceException.conflict("Category slug already exists");
        }

        Category parent = null;
        if (request.parentId() != null) {
            parent = getCategory(request.parentId());
        }

        var category = Category.builder()
                .name(request.name())
                .slug(slug)
                .description(request.description())
                .parent(parent)
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .commissionRate(request.commissionRate())
                .build();

        return toCategoryResponse(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public java.util.List<ProductDtos.CategoryResponse> listCategories() {
        return categoryRepository.findAll().stream().map(this::toCategoryResponse).toList();
    }

    public Product getActiveProduct(UUID productId) {
        var product = productRepository.findById(productId)
                .orElseThrow(() -> MarketplaceException.notFound("Product", productId));
        if (product.getStatus() != ProductStatus.ACTIVE) {
            throw MarketplaceException.badRequest("Product is not available for purchase");
        }
        return product;
    }

    private Category getCategory(UUID categoryId) {
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> MarketplaceException.notFound("Category", categoryId));
    }

    private String generateUniqueSlug(String title) {
        var base = slugify(title);
        var slug = base;
        int counter = 1;
        while (productRepository.findBySlug(slug).isPresent()) {
            slug = base + "-" + counter++;
        }
        return slug;
    }

    private String slugify(String input) {
        var normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-");
        return normalized.isBlank() ? UUID.randomUUID().toString().substring(0, 8) : normalized;
    }

    private String buildSearchVector(String title, String description, String brand) {
        return String.join(" ",
                title != null ? title : "",
                description != null ? description : "",
                brand != null ? brand : "").trim();
    }

    public ProductDtos.ProductResponse toPublicResponse(Product product) {
        return toResponse(product);
    }

    private ProductDtos.ProductResponse toResponse(Product product) {
        return ProductDtos.ProductResponse.builder()
                .id(product.getId())
                .merchantId(product.getMerchant().getId())
                .merchantName(product.getMerchant().getBusinessName())
                .categoryId(product.getCategory().getId())
                .categoryName(product.getCategory().getName())
                .title(product.getTitle())
                .slug(product.getSlug())
                .description(product.getDescription())
                .sku(product.getSku())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .stockQuantity(product.getStockQuantity())
                .favoriteCount(product.getFavoriteCount())
                .status(product.getStatus())
                .brand(product.getBrand())
                .imageUrls(product.getImageUrls())
                .build();
    }

    private ProductDtos.CategoryResponse toCategoryResponse(Category category) {
        return ProductDtos.CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .parentId(category.getParent() != null ? category.getParent().getId() : null)
                .active(category.isActive())
                .sortOrder(category.getSortOrder())
                .build();
    }
}
