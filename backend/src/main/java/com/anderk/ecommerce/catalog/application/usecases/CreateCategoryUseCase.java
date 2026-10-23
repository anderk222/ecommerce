package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.view.CategoryView;

/**
 * The use case that creates a category (HU-20).
 */
public interface CreateCategoryUseCase {

    /**
     * Creates a category. The slug is generated from the name and is unique.
     *
     * @throws com.anderk.ecommerce.domain.error.ValidationException if the name is empty
     */
    CategoryView create(String name);
}
