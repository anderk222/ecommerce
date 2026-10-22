package com.anderk.ecommerce.catalog.application;

import com.anderk.ecommerce.catalog.application.view.CategoryView;
import com.anderk.ecommerce.domain.error.ConflictException;
import com.anderk.ecommerce.domain.error.NotFoundException;

/**
 * The inbound port of the **writes** of the categories.
 *
 * <p>It is the contract of the administration of the catalogue (HU-20).
 */
public interface CategoryCommandService {

    /**
     * Creates a category. The slug is generated from the name and is unique.
     *
     * @throws com.anderk.ecommerce.domain.error.ValidationException if the name is empty
     */
    CategoryView create(String name);

    /**
     * Renames a category. The slug does not change: the links that are already
     * published keep working.
     *
     * @throws NotFoundException if the category does not exist
     */
    CategoryView update(Long id, String name);

    /**
     * Deletes a category.
     *
     * @throws NotFoundException if the category does not exist
     * @throws ConflictException if the category still holds items
     */
    void delete(Long id);
}
