package com.toanlv.flashsale.product.service;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.toanlv.flashsale.product.domain.Product;
import com.toanlv.flashsale.product.domain.ProductStatus;
import com.toanlv.flashsale.product.dto.CreateProductRequest;
import com.toanlv.flashsale.product.dto.ProductDto;
import com.toanlv.flashsale.product.dto.UpdateProductRequest;

public interface IProductService {
  Page<ProductDto> findActive(UUID categoryId, String search, Pageable pageable);

  ProductDto findById(UUID id);

  ProductDto update(UUID id, UpdateProductRequest request);

  ProductDto changeStatus(UUID id, ProductStatus status);

  ProductDto create(CreateProductRequest request);

  Product findActiveProduct(UUID id);
}
