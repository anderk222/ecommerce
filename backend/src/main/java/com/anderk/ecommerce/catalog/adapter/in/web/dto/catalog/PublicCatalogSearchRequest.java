package com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog;

import static com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogQueryParams.enumOrElse;
import static com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogQueryParams.pageOrFirst;

import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.policy.StockPolicy;
import com.anderk.ecommerce.catalog.domain.search.CatalogSearchCriteria;
import com.anderk.ecommerce.catalog.domain.search.CatalogSort;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;

/**
 * The query parameters of the public listing ({@code GET /api/public/catalog}).
 *
 * <p>Every parameter is optional.
 *
 * @param type {@code PRODUCT}, {@code SERVICE}, or nothing for both
 * @param q the free text, matched against the name and the description
 * @param category the slug of the category
 * @param sort {@code newest}, {@code oldest}, {@code price_asc}, {@code price_desc},
 *             {@code name_asc}, {@code name_desc} or {@code best_selling}
 * @param onSale only the items with a discount in force
 * @param lowStock only the items at or below the threshold
 * @param lowStockThreshold the threshold of {@code lowStock}, 5 when not given
 * @param page the 0-based page number
 * @param size the page size, at most 50
 */
public record PublicCatalogSearchRequest(
        String type,
        String q,
        String category,
        String sort,
        Boolean onSale,
        Boolean lowStock,
        Integer lowStockThreshold,
        Integer page,
        Integer size) {

    public CatalogSearchCriteria toCriteria() {
        return CatalogSearchCriteria.forPublic(
                this.q,
                enumOrElse(ItemType.class, this.type, null),
                this.category,
                Boolean.TRUE.equals(this.onSale),
                stockAtMost(),
                enumOrElse(CatalogSort.class, this.sort, CatalogSort.DEFAULT));
    }

    public PageRequestDto toPageRequest() {
        return PageRequestDto.of(pageOrFirst(this.page), this.size);
    }

    /** The stock limit of the {@code lowStock} filter, or null when the filter is off. */
    private Integer stockAtMost() {
        if (!Boolean.TRUE.equals(this.lowStock)) {
            return null;
        }
        return this.lowStockThreshold != null
                ? this.lowStockThreshold
                : StockPolicy.DEFAULT_LOW_STOCK_THRESHOLD;
    }
}
