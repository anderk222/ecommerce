package com.anderk.ecommerce.tracking.domain.model;

/**
 * The events of the visitor that the CRM cares about (HU-09).
 *
 * <p>A generic page view is **not** one of them: it would be noise.
 */
public enum EventType {

    /** The visitor searched something. */
    SEARCH,

    /** The visitor opened the detail of an item. */
    PRODUCT_VIEW,

    /** The visitor added an item to the cart. */
    ADD_TO_CART,

    /** The visitor removed an item from the cart. */
    REMOVE_FROM_CART,

    /** The visitor completed a purchase. */
    PURCHASE,

    /** The visitor asked to be notified when the stock comes back. */
    STOCK_SUBSCRIBE,

    /** The visitor asked for more information. */
    INFO_REQUEST
}
