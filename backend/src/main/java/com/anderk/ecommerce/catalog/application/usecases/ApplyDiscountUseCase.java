package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.command.ApplyDiscountCommand;

/**
 * The use case that applies a discount to an item (HU-19).
 */
public interface ApplyDiscountUseCase {

    /**
     * Applies a discount. There is only one discount in force per item: the
     * new one replaces the previous one.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     */
    void apply(Long id, ApplyDiscountCommand command);
}
