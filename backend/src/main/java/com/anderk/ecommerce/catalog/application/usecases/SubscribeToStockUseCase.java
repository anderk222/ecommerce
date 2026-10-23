package com.anderk.ecommerce.catalog.application.usecases;

import com.anderk.ecommerce.catalog.application.command.SubscribeToStockCommand;

/**
 * The use case that subscribes a visitor to the notice that an item comes
 * back (HU-08).
 */
public interface SubscribeToStockUseCase {

    /**
     * Subscribes a visitor to the notice.
     *
     * <p>It creates the lead — with the origin {@code STOCK_NOTIFY} — and the
     * subscription. There is one subscription per lead and item.
     */
    void subscribe(SubscribeToStockCommand command);
}
