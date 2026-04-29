package com.toanlv.flashsale.product.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.toanlv.flashsale.product.dto.CategoryDto;
import com.toanlv.flashsale.product.dto.ProductDto;
import com.toanlv.flashsale.product.service.ICategoryService;
import com.toanlv.flashsale.product.service.IProductService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "Products", description = "Browse product catalog")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

  private final IProductService productService;
  private final ICategoryService categoryService;

  @Operation(
      summary = "List active products",
      description =
          "Paginated list. Optional filters: categoryId, search. "
              + "Default sort: createdAt DESC.")
  @GetMapping
  public ResponseEntity<Page<ProductDto>> list(
      @RequestParam(required = false) UUID categoryId,
      @RequestParam(required = false) String search,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "20") int size) {

    // Build Pageable manually — avoids Swagger sending "string"
    // as sort field via the @PageableDefault annotation.
    // Sort by createdAt DESC is a safe default for product listing.
    var pageable =
        PageRequest.of(
            page,
            Math.min(size, 100), // cap at 100 per page
            Sort.by(Sort.Direction.DESC, "createdAt"));

    return ResponseEntity.ok(productService.findActive(categoryId, search, pageable));
  }

  @Operation(summary = "Get product by ID")
  @GetMapping("/{id}")
  public ResponseEntity<ProductDto> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(productService.findById(id));
  }

  @Operation(summary = "List all categories")
  @GetMapping("/categories")
  public ResponseEntity<List<CategoryDto>> listCategories() {
    return ResponseEntity.ok(categoryService.findAll());
  }
}
