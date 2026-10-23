package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CatalogItemSummary;
import com.anderk.ecommerce.catalog.domain.search.CatalogSearchCriteria;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.domain.pagination.PageResponse;

/**
 * The use case that searches the public catalog (HU-02 to HU-04).
 */
public interface SearchCatalogUseCase {

    /**
     * The published items that match the criteria, paginated.
     *
     * <p>Never null. Only the items that are active and not deleted.
     */
    PageResponse<CatalogItemSummary> search(CatalogSearchCriteria criteria, PageRequestDto page);
}
