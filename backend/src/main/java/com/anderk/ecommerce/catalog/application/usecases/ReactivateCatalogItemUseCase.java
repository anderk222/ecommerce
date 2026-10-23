package com.anderk.ecommerce.catalog.application.usecases;

/**
 * The use case that publishes again an item that was deactivated (HU-17).
 */
public interface ReactivateCatalogItemUseCase {

    /**
     * Reactivates an item that was deactivated.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     */
    void reactivate(Long id);
}
