package com.anderk.ecommerce.catalog.application.usecases;

import java.util.List;

import com.anderk.ecommerce.catalog.application.view.CategoryView;

/**
 * The use case that lists the categories with their number of items (HU-20).
 */
public interface ListCategoriesUseCase {

    /** The categories, with the number of published items each one holds. */
    List<CategoryView> list();
}
