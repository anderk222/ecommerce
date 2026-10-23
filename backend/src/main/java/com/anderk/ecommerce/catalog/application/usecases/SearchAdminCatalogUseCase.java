package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminView;
import com.anderk.ecommerce.catalog.domain.search.CatalogSearchCriteria;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.domain.pagination.PageResponse;

/**
 * The use case behind the unified listing of the administration (HU-14).
 */
public interface SearchAdminCatalogUseCase {

    /**
     * The items of the administration: the inactive ones and, when the criteria
     * ask for them, the deleted ones.
     */
    PageResponse<CatalogItemAdminView> search(CatalogSearchCriteria criteria, PageRequestDto page);
}
