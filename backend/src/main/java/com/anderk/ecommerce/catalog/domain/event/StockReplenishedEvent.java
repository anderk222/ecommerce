package com.anderk.ecommerce.catalog.domain.event;

/**
 * The stock of an item went from zero to a number above zero.
 *
 * <p>It is the signal that the pending subscriptions of the item have to be
 * notified (HU-08). The catalogue publishes it; whoever cares about it
 * listens. The catalogue does not know who listens.
 *
 * @param itemId the item that was replenished
 * @param previousStock the stock before, zero
 * @param newStock the stock after
 */
public record StockReplenishedEvent(Long itemId, int previousStock, int newStock) {
}
