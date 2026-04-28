package com.toanlv.flashsale.product.service;

import java.util.List;
import java.util.UUID;

import com.toanlv.flashsale.product.dto.CategoryDto;
import com.toanlv.flashsale.product.dto.CreateCategoryRequest;

public interface ICategoryService {
  List<CategoryDto> findAll();

  CategoryDto findById(UUID id);

  CategoryDto create(CreateCategoryRequest request);
}
