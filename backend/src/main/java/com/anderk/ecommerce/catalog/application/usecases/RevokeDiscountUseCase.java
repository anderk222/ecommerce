package com.anderk.ecommerce.catalog.application.usecases;

/**
 * The use case that revokes the discount in force of an item (HU-19).
 */
public interface RevokeDiscountUseCase {

    /**
     * Revokes the discount in force. The effect is immediate.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     * @throws com.anderk.ecommerce.domain.error.ConflictException if the item has no discount in force
     */
    void revoke(Long id);
}
