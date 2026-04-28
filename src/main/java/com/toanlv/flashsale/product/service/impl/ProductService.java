package com.toanlv.flashsale.product.service.impl;

import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.inventory.domain.Inventory;
import com.toanlv.flashsale.inventory.repository.InventoryRepository;
import com.toanlv.flashsale.product.domain.Product;
import com.toanlv.flashsale.product.domain.ProductStatus;
import com.toanlv.flashsale.product.dto.CreateProductRequest;
import com.toanlv.flashsale.product.dto.ProductDto;
import com.toanlv.flashsale.product.dto.UpdateProductRequest;
import com.toanlv.flashsale.product.repository.CategoryRepository;
import com.toanlv.flashsale.product.repository.ProductRepository;
import com.toanlv.flashsale.product.service.IProductService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService implements IProductService {

  private final ProductRepository productRepository;
  private final CategoryRepository categoryRepository;
  private final InventoryRepository inventoryRepository;

  // ----------------------------------------------------------------
  // Public read
  /**
   * Paginated product list with optional category and search filters. Only returns ACTIVE products
   * for public access.
   */
  @Override
  @Cacheable(
      value = "product-catalog",
      key =
          "#categoryId + ':' + #search + ':' + #pageable.pageNumber" + "+ ':' + #pageable.pageSize")
  @Transactional(readOnly = true)
  public Page<ProductDto> findActive(UUID categoryId, String search, Pageable pageable) {
    return productRepository
        .findByFilters(ProductStatus.ACTIVE, categoryId, search, pageable)
        .map(ProductDto::from);
  }

  // ----------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public ProductDto findById(UUID id) {
    return productRepository
        .findByIdWithCategory(id)
        .filter(Product::isActive)
        .map(ProductDto::from)
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
  }

  // ----------------------------------------------------------------
  // Admin operations
  // ----------------------------------------------------------------

  /** Update product metadata. SKU is immutable after creation. */
  @Override
  @CacheEvict(value = "product-catalog", allEntries = true)
  @Transactional
  public ProductDto update(UUID id, UpdateProductRequest request) {
    var product =
        productRepository
            .findByIdWithCategory(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

    var category =
        request.categoryId() != null
            ? categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND))
            : null;

    product.update(
        request.name(), request.description(), request.basePrice(), request.imageUrl(), category);

    return ProductDto.from(productRepository.save(product));
  }

  /** Change product status. Soft delete via DISCONTINUED — preserves order history references. */
  @Override
  @CacheEvict(value = "product-catalog", allEntries = true)
  @Transactional
  public ProductDto changeStatus(UUID id, ProductStatus status) {
    var product =
        productRepository
            .findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));

    product.changeStatus(status);
    return ProductDto.from(productRepository.save(product));
  }

  /** Create a new product. Automatically creates a corresponding inventory record with 0 stock. */
  @Override
  @CacheEvict(value = "product-catalog", allEntries = true)
  @Transactional
  public ProductDto create(CreateProductRequest request) {
    if (productRepository.existsBySku(request.sku())) {
      throw new BusinessException(ErrorCode.PRODUCT_ALREADY_EXISTS);
    }

    var category =
        request.categoryId() != null
            ? categoryRepository
                .findById(request.categoryId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND))
            : null;

    var product =
        Product.create(
            request.sku(),
            request.name(),
            request.description(),
            request.basePrice(),
            request.imageUrl(),
            category);

    var saved = productRepository.save(product);

    // Create inventory record with zero initial stock
    inventoryRepository.save(Inventory.init(saved.getId()));

    return ProductDto.from(saved);
  }

  // ----------------------------------------------------------------
  // Internal — used by flashsale domain
  // ----------------------------------------------------------------

  @Override
  @Transactional(readOnly = true)
  public Product findActiveProduct(UUID id) {
    return productRepository
        .findByIdWithCategory(id)
        .filter(Product::isActive)
        .orElseThrow(() -> new BusinessException(ErrorCode.PRODUCT_NOT_FOUND));
  }
}
