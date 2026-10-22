package com.anderk.ecommerce.catalog.application;

import com.anderk.ecommerce.catalog.application.command.ApplyDiscountCommand;
import com.anderk.ecommerce.catalog.application.command.CreateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.command.UpdateCatalogItemCommand;
import com.anderk.ecommerce.catalog.application.view.CatalogItemDetail;

/**
 * The inbound port of the **writes** of the catalog.
 *
 * <p>It is the contract of §12.4. Every mutation of the catalog goes through
 * it, so there is exactly one door to the data of the catalogue.
 */
public interface CatalogCommandService {

    /**
     * Creates an item. The slug is generated from the name and is unique.
     *
     * @throws com.anderk.ecommerce.domain.error.ValidationException if the price or the stock are negative
     */
    CatalogItemDetail create(CreateCatalogItemCommand command);

    /**
     * Edits an item.
     *
     * <p>If the stock goes from 0 to more than 0, the pending stock
     * subscriptions are notified.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the item does not exist
     */
    CatalogItemDetail update(Long id, UpdateCatalogItemCommand command);

    /** Deactivates an item. It disappears from the public catalog and can be reactivated. */
    void deactivate(Long id);

    /** Reactivates an item that was deactivated. */
    void reactivate(Long id);

    /** Logically deletes an item. The order history and the audit history survive. */
    void delete(Long id);

    /**
     * Applies a discount. There is only one discount in force per item: the
     * new one replaces the previous one.
     */
    void applyDiscount(Long id, ApplyDiscountCommand command);

    /** Revokes the discount in force. The effect is immediate. */
    void revokeDiscount(Long id);
}
