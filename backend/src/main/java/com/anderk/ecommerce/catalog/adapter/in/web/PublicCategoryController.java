package com.anderk.ecommerce.catalog.adapter.in.web;

import java.util.List;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.CategoryResponse;
import com.anderk.ecommerce.catalog.application.CatalogQueryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public categories, with the number of published items each one holds.
 */
@RestController
@RequestMapping("/api/public/categories")
public class PublicCategoryController {

    private final CatalogQueryService catalogQueryService;

    public PublicCategoryController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    @GetMapping
    public List<CategoryResponse> categories() {
        return CatalogApiMapper.toResponses(this.catalogQueryService.listCategories());
    }

    /**
     * The category with that slug — the listing of {@code /categorias/{slug}}
     * asks for it to write its title.
     */
    @GetMapping("/{slug}")
    public CategoryResponse category(@PathVariable String slug) {
        return CatalogApiMapper.toResponse(this.catalogQueryService.getCategoryBySlug(slug));
    }
}
