package com.anderk.ecommerce.catalog.domain.model;

/**
 * What the stock column means for a given item.
 *
 * <p>Mission section 7.2 is explicit: {@code stock = NULL} means *stock is not
 * tracked*, and only {@code stock = 0} means *out of stock*. The two are
 * never conflated — a service has no stock and is never out of stock.
 */
public enum StockState {

    /** The item does not track stock ({@code stock IS NULL}). */
    NOT_TRACKED,

    /** The item is available and has plenty of units. */
    IN_STOCK,

    /** The item is available but close to running out. */
    LOW_STOCK,

    /** The item has zero units and cannot be bought. */
    OUT_OF_STOCK
}
