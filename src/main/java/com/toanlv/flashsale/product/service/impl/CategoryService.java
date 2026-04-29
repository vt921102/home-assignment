package com.toanlv.flashsale.product.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.toanlv.flashsale.common.exception.BusinessException;
import com.toanlv.flashsale.common.exception.ErrorCode;
import com.toanlv.flashsale.product.domain.ProductCategory;
import com.toanlv.flashsale.product.dto.CategoryDto;
import com.toanlv.flashsale.product.dto.CreateCategoryRequest;
import com.toanlv.flashsale.product.repository.CategoryRepository;
import com.toanlv.flashsale.product.service.ICategoryService;

@Service
public class CategoryService implements ICategoryService {

  private final CategoryRepository repository;

  public CategoryService(CategoryRepository repository) {
    this.repository = repository;
  }

  /** List all categories with parent information. Cached — categories change rarely. */
  @Override
  @Cacheable("category-tree")
  @Transactional(readOnly = true)
  public List<CategoryDto> findAll() {
    return new ArrayList<>(repository.findAllWithParent().stream().map(CategoryDto::from).toList());
  }

  @Override
  @Transactional(readOnly = true)
  public CategoryDto findById(UUID id) {
    return repository
        .findById(id)
        .map(CategoryDto::from)
        .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
  }

  @Override
  @CacheEvict(value = "category-tree", allEntries = true)
  @Transactional
  public CategoryDto create(CreateCategoryRequest request) {
    if (repository.existsByName(request.name())) {
      throw new BusinessException(
          ErrorCode.DUPLICATE_REQUEST,
          "Category with name '" + request.name() + "' already exists");
    }

    ProductCategory parent = null;
    if (request.parentId() != null) {
      parent =
          repository
              .findById(request.parentId())
              .orElseThrow(() -> new BusinessException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    var category = ProductCategory.create(request.name(), parent);
    return CategoryDto.from(repository.save(category));
  }
}
