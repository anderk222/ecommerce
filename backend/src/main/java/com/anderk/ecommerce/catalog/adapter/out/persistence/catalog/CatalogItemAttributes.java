package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

/**
 * The attribute names of {@link CatalogItemJpaEntity}, as the Criteria API and
 * {@code Sort} spell them (the Java field, not the column).
 *
 * <p>They are strings because the Criteria API reads attributes by name. Keeping
 * them here means that renaming a field of the entity breaks one file, and not
 * every query that mentions it.
 */
final class CatalogItemAttributes {

    static final String ID = "id";
    static final String TYPE = "type";
    static final String NAME = "name";
    static final String PRICE = "price";
    static final String STOCK = "stock";
    static final String SEARCH_TEXT = "searchText";
    static final String ACTIVE = "active";
    static final String DELETED_AT = "deletedAt";
    static final String CREATED_AT = "createdAt";
    static final String SALES_COUNT = "salesCount";

    private CatalogItemAttributes() {
    }
}
