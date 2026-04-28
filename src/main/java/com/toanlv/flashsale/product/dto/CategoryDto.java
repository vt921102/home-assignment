package com.toanlv.flashsale.product.dto;

import java.util.UUID;

import com.toanlv.flashsale.product.domain.ProductCategory;

public record CategoryDto(UUID id, String name, UUID parentId, String parentName) {
  public static CategoryDto from(ProductCategory category) {
    return new CategoryDto(
        category.getId(),
        category.getName(),
        category.getParent() != null ? category.getParent().getId() : null,
        category.getParent() != null ? category.getParent().getName() : null);
  }
}
