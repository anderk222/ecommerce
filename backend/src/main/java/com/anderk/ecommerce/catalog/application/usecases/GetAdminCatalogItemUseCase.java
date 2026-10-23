package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminDetailView;

/**
 * The use case that reads an item for the form of the edition (HU-16).
 */
public interface GetAdminCatalogItemUseCase {

    /**
     * The item with that identifier, whatever its state.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     */
    CatalogItemAdminDetailView getById(Long id);
}
