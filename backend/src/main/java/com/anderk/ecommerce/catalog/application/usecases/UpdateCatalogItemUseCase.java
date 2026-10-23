package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.command.UpdateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;

/**
 * The use case that edits an item of the catalog (HU-16).
 */
public interface UpdateCatalogItemUseCase {

    /**
     * Edits an item.
     *
     * <p>If the stock goes from 0 to more than 0, the pending stock
     * subscriptions are notified.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     */
    CatalogItemDetail update(Long id, UpdateCatalogItemCommand command);
}
