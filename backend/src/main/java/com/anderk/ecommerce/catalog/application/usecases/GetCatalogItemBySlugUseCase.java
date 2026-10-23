package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;

/**
 * The use case that reads the detail page of an item (HU-05).
 */
public interface GetCatalogItemBySlugUseCase {

    /**
     * The published item with that slug.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the slug does not exist, or the
     *         item is inactive or deleted
     */
    CatalogItemDetail getBySlug(String slug);
}
