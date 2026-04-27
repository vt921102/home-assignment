package com.toanlv.flashsale.product.controller;


import com.toanlv.flashsale.product.dto.ProductDto;
import com.toanlv.flashsale.product.service.CategoryService;
import com.toanlv.flashsale.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@Tag(name = "Products", description = "Browse product catalog")
@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService  productService;
    private final CategoryService categoryService;

    public ProductController(
            ProductService productService,
            CategoryService categoryService) {
        this.productService  = productService;
        this.categoryService = categoryService;
    }

    @Operation(summary = "List active products",
            description = "Paginated list with optional category and search filters.")
    @GetMapping
    public ResponseEntity<Page<ProductDto>> list(
            @RequestParam(required = false) UUID categoryId,
            @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(
                productService.findActive(categoryId, search, pageable));
    }

    @Operation(summary = "Get product by ID")
    @GetMapping("/{id}")
    public ResponseEntity<ProductDto> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.findById(id));
    }
}
