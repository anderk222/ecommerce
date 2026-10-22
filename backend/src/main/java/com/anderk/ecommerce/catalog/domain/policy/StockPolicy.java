package com.anderk.ecommerce.catalog.domain.policy;

import com.anderk.ecommerce.catalog.domain.model.StockState;

/**
 * The rules about stock.
 *
 * <p>Mission section 7.2 draws the line that is easy to get wrong:
 * {@code stock = NULL} means *stock is not tracked*, and only
 * {@code stock = 0} means *out of stock*. A service with no stock column is
 * never out of stock and never has to be replenished.
 */
public class StockPolicy {

    /** The threshold below which a tracked item is "low stock". */
    public static final int DEFAULT_LOW_STOCK_THRESHOLD = 5;

    /**
     * The state of a stock level.
     *
     * @param stock the units available, null when stock is not tracked
     * @param lowStockThreshold the level at or below which the item is low
     */
    public StockState stateOf(Integer stock, int lowStockThreshold) {
        if (stock == null) {
            return StockState.NOT_TRACKED;
        }
        if (stock <= 0) {
            return StockState.OUT_OF_STOCK;
        }
        return stock <= lowStockThreshold ? StockState.LOW_STOCK : StockState.IN_STOCK;
    }

    /** Whether the item can be sold in the requested quantity. */
    public boolean canDecrease(Integer stock, int quantity) {
        if (quantity <= 0) {
            return false;
        }
        if (stock == null) {
            return true;
        }
        return stock >= quantity;
    }
}
