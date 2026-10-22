package com.anderk.ecommerce.catalog.domain.port;

/**
 * The orderings the public catalog offers.
 *
 * <p>HU-03 and HU-04 ask for ordering by price, by name and by "most bought".
 */
public enum CatalogSort {

    /** The newest first — the default. */
    NEWEST("created_at", false),

    /** The oldest first. */
    OLDEST("created_at", true),

    /** Cheapest first. */
    PRICE_ASC("price", false),

    /** Most expensive first. */
    PRICE_DESC("price", true),

    /** Alphabetically, A to Z. */
    NAME_ASC("name", false),

    /** Alphabetically, Z to A. */
    NAME_DESC("name", true),

    /** The most bought first. */
    BEST_SELLING("best_selling", true);

    private final String property;

    private final boolean descending;

    CatalogSort(String property, boolean descending) {
        this.property = property;
        this.descending = descending;
    }

    public String property() {
        return this.property;
    }

    public boolean descending() {
        return this.descending;
    }
}
