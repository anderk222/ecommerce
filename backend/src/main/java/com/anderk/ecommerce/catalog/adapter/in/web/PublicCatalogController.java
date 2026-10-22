package com.anderk.ecommerce.catalog.adapter.in.web;

import java.util.List;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.CatalogItemDetailResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.CatalogItemSummaryResponse;
import com.anderk.ecommerce.catalog.application.CatalogQueryService;
import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.policy.StockPolicy;
import com.anderk.ecommerce.catalog.domain.port.CatalogSearchCriteria;
import com.anderk.ecommerce.catalog.domain.port.CatalogSort;
import com.anderk.ecommerce.shared.pagination.PageRequestDto;
import com.anderk.ecommerce.shared.pagination.PageResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The public catalog: the listing and the detail of an item.
 *
 * <p>The routes and the query parameters are the ones of §12.5. The controller
 * only translates HTTP into a use case call — no business rule lives here.
 */
@RestController
@RequestMapping("/api/public/catalog")
public class PublicCatalogController {

    private final CatalogQueryService catalogQueryService;

    public PublicCatalogController(CatalogQueryService catalogQueryService) {
        this.catalogQueryService = catalogQueryService;
    }

    /**
     * The listing.
     *
     * @param type {@code PRODUCT}, {@code SERVICE}, or nothing for both
     * @param q the free text, matched against the name and the description
     * @param category the slug of the category
     * @param sort {@code newest}, {@code oldest}, {@code price_asc}, {@code price_desc},
     *             {@code name_asc}, {@code name_desc} or {@code best_selling}
     * @param onSale only the items with a discount in force
     * @param lowStock only the items at or below the threshold
     * @param lowStockThreshold the threshold of the low stock filter
     * @param page the 0-based page number
     * @param size the page size, at most 50
     */
    @GetMapping
    public PageResponse<CatalogItemSummaryResponse> search(
            @RequestParam(required = false) String type,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "false") boolean onSale,
            @RequestParam(defaultValue = "false") boolean lowStock,
            @RequestParam(defaultValue = "" + StockPolicy.DEFAULT_LOW_STOCK_THRESHOLD) int lowStockThreshold,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(required = false) Integer size) {

        CatalogSearchCriteria criteria = CatalogSearchCriteria.publicCatalog(
                q,
                parseType(type),
                blankToNull(category),
                parseSort(sort),
                onSale,
                lowStock,
                lowStockThreshold);

        return this.catalogQueryService
                .search(criteria, PageRequestDto.of(page, size))
                .map(CatalogApiMapper::toResponse);
    }

    /** The slugs of the published items — the sitemap asks for them. */
    @GetMapping("/slugs")
    public List<String> slugs() {
        return this.catalogQueryService.listActiveSlugs();
    }

    /** The detail of an item, by its slug. */
    @GetMapping("/{slug}")
    public CatalogItemDetailResponse detail(@PathVariable String slug) {
        return CatalogApiMapper.toResponse(this.catalogQueryService.getPublicBySlug(slug));
    }

    private ItemType parseType(String type) {
        if (blankToNull(type) == null) {
            return null;
        }
        try {
            return ItemType.valueOf(type.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private CatalogSort parseSort(String sort) {
        if (blankToNull(sort) == null) {
            return CatalogSort.NEWEST;
        }
        try {
            return CatalogSort.valueOf(sort.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return CatalogSort.NEWEST;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
