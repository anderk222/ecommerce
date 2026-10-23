package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CategoryView;

/**
 * The use case that reads a category by its slug.
 */
public interface GetCategoryBySlugUseCase {

    /**
     * The category with that slug.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the category does not exist
     */
    CategoryView getBySlug(String slug);
}
