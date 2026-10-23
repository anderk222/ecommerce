package com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog;

import static com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogQueryParams.enumOrElse;
import static com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogQueryParams.pageOrFirst;

import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.search.CatalogSearchCriteria;
import com.anderk.ecommerce.catalog.domain.search.CatalogSort;
import com.anderk.ecommerce.catalog.domain.search.ItemStateFilter;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;

/**
 * The query parameters of the administration listing
 * ({@code GET /api/admin/catalog}, HU-14).
 *
 * <p>Every parameter is optional.
 *
 * @param type {@code PRODUCT}, {@code SERVICE}, or nothing for both
 * @param q the free text
 * @param category the slug of the category
 * @param sort the ordering, the same values as the public listing
 * @param state {@code ALL} (the default), {@code ACTIVE}, {@code INACTIVE} or {@code DELETED}
 * @param page the 0-based page number
 * @param size the page size
 */
public record AdminCatalogSearchRequest(
        String type,
        String q,
        String category,
        String sort,
        String state,
        Integer page,
        Integer size) {

    public CatalogSearchCriteria toCriteria() {
        return CatalogSearchCriteria.forAdmin(
                this.q,
                enumOrElse(ItemType.class, this.type, null),
                this.category,
                enumOrElse(ItemStateFilter.class, this.state, ItemStateFilter.ALL),
                enumOrElse(CatalogSort.class, this.sort, CatalogSort.DEFAULT));
    }

    public PageRequestDto toPageRequest() {
        return PageRequestDto.of(pageOrFirst(this.page), this.size);
    }
}
