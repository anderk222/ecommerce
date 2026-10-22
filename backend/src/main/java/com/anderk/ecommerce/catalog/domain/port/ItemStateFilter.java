package com.anderk.ecommerce.catalog.domain.port;

/**
 * The lifecycle states the administration listing can filter by (HU-14).
 */
public enum ItemStateFilter {

    /** Every item, whatever its state. */
    ALL,

    /** Published and not deleted. */
    ACTIVE,

    /** Deactivated but not deleted. */
    INACTIVE,

    /** Logically deleted. */
    DELETED
}
