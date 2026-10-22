package com.anderk.ecommerce.catalog.domain.model;

/**
 * The kind of thing the catalog sells.
 *
 * <p>Products and services are one entity with a type (mission section 7.1):
 * every catalog use case applies to both unless it says otherwise.
 */
public enum ItemType {

    /** A physical thing with a stock. */
    PRODUCT,

    /** Something that is delivered, not shipped. */
    SERVICE;

    public boolean isService() {
        return this == SERVICE;
    }

    public boolean isProduct() {
        return this == PRODUCT;
    }
}
