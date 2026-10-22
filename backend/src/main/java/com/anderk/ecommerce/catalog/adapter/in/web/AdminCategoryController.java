package com.anderk.ecommerce.catalog.adapter.in.web;

import java.util.List;

import com.anderk.ecommerce.catalog.application.CategoryCommandService;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.CategoryResponse;
import com.anderk.ecommerce.catalog.application.CatalogQueryService;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The administration of the categories (HU-20).
 *
 * <p>The route is under `/api/admin`, so it needs the staff role — the
 * filter that checks it arrives in P10.
 */
@RestController
@RequestMapping("/api/admin/categories")
public class AdminCategoryController {

    private final CatalogQueryService catalogQueryService;

    private final CategoryCommandService categoryCommandService;

    public AdminCategoryController(CatalogQueryService catalogQueryService,
                                   CategoryCommandService categoryCommandService) {
        this.catalogQueryService = catalogQueryService;
        this.categoryCommandService = categoryCommandService;
    }

    /** Every category, with the number of published items it holds. */
    @GetMapping
    public List<CategoryResponse> list() {
        return CatalogApiMapper.toResponses(this.catalogQueryService.listCategories());
    }

    /** Creates a category. The slug is generated and is unique. */
    @PostMapping
    public ResponseEntity<CategoryResponse> create(@Valid @RequestBody CategoryRequest request) {
        CategoryResponse created = CatalogApiMapper.toResponse(
                this.categoryCommandService.create(request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** Renames a category. */
    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long id, @Valid @RequestBody CategoryRequest request) {
        return CatalogApiMapper.toResponse(this.categoryCommandService.update(id, request.name()));
    }

    /**
     * Deletes a category.
     *
     * <p>A category that still holds items answers 409: the items have to be
     * moved first (HU-20).
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        this.categoryCommandService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * The body of the creation and of the edition of a category.
     *
     * @param name the display name
     */
    public record CategoryRequest(@NotBlank String name) {
    }
}
