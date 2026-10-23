package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CategoryView;

/**
 * The use case that renames a category (HU-20).
 */
public interface UpdateCategoryUseCase {

    /**
     * Renames a category. The slug does not change: the links that are already
     * published keep working.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the category does not exist
     */
    CategoryView update(Long id, String name);
}
