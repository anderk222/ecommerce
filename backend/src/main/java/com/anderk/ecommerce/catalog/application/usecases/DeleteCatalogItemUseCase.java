package com.anderk.ecommerce.catalog.application.usecases;

/**
 * The use case that logically deletes an item (HU-18).
 */
public interface DeleteCatalogItemUseCase {

    /**
     * Logically deletes an item. The order history and the audit history
     * survive.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     */
    void delete(Long id);
}
