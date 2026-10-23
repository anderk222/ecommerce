package com.anderk.ecommerce.catalog.application.usecases;

/**
 * The use case that deletes a category (HU-20).
 */
public interface DeleteCategoryUseCase {

    /**
     * Deletes a category.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the category does not exist
     * @throws com.anderk.ecommerce.domain.error.ConflictException if the category still holds items
     */
    void delete(Long id);
}
