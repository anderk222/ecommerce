package com.anderk.ecommerce.catalog.application.usecases;

/**
 * The use case that takes an item out of the public catalog (HU-17).
 */
public interface DeactivateCatalogItemUseCase {

    /**
     * Deactivates an item. It disappears from the public catalog and can be
     * reactivated.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     */
    void deactivate(Long id);
}
