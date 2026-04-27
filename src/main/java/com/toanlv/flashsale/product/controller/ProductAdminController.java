package com.toanlv.flashsale.product.controller;

import com.toanlv.flashsale.product.domain.ProductStatus;
import com.toanlv.flashsale.product.dto.CategoryDto;
import com.toanlv.flashsale.product.dto.CreateCategoryRequest;
import com.toanlv.flashsale.product.dto.CreateProductRequest;
import com.toanlv.flashsale.product.dto.ProductDto;
import com.toanlv.flashsale.product.dto.UpdateProductRequest;
import com.toanlv.flashsale.product.service.CategoryService;
import com.toanlv.flashsale.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Tag(name = "Admin — Products",
        description = "Product and category management")
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
public class ProductAdminController {

    private final ProductService  productService;
    private final CategoryService categoryService;

    public ProductAdminController(
            ProductService productService,
            CategoryService categoryService) {
        this.productService  = productService;
        this.categoryService = categoryService;
    }

    // ----------------------------------------------------------------
    // Products
    // ----------------------------------------------------------------

    @Operation(summary = "Create product")
    @PostMapping("/products")
    public ResponseEntity<ProductDto> create(
            @Valid @RequestBody CreateProductRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.create(request));
    }

    @Operation(summary = "Update product")
    @PutMapping("/products/{id}")
    public ResponseEntity<ProductDto> update(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @Operation(summary = "Change product status",
            description = "Valid values: ACTIVE, INACTIVE, DISCONTINUED")
    @PatchMapping("/products/{id}/status")
    public ResponseEntity<ProductDto> changeStatus(
            @PathVariable UUID id,
            @RequestParam ProductStatus status) {
        return ResponseEntity.ok(productService.changeStatus(id, status));
    }

    // ----------------------------------------------------------------
    // Categories
    // ----------------------------------------------------------------

    @Operation(summary = "List all categories")
    @GetMapping("/categories")
    public ResponseEntity<List<CategoryDto>> listCategories() {
        return ResponseEntity.ok(categoryService.findAll());
    }

    @Operation(summary = "Create category")
    @PostMapping("/categories")
    public ResponseEntity<CategoryDto> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.create(request));
    }
}
