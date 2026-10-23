package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.command.CreateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;

/**
 * The use case that creates an item of the catalog (HU-15).
 */
public interface CreateCatalogItemUseCase {

    /**
     * Creates an item. The slug is generated from the name and is unique.
     *
     * @throws com.anderk.ecommerce.domain.error.ValidationException if the price or the stock are negative
     */
    CatalogItemDetail create(CreateCatalogItemCommand command);
}
