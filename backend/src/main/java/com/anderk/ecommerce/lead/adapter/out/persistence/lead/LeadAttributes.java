package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

/**
 * The attribute names of {@link LeadJpaEntity}, as the Criteria API and
 * {@code Sort} spell them (the Java field, not the column).
 *
 * <p>They are strings because the Criteria API reads attributes by name. Keeping
 * them here means that renaming a field of the entity breaks one file, and not
 * every query that mentions it.
 */
final class LeadAttributes {

    static final String ID = "id";
    static final String NAME = "name";
    static final String EMAIL = "email";
    static final String PHONE = "phone";
    static final String PROVINCE = "province";
    static final String STATUS = "status";
    static final String SOURCES = "sources";
    static final String SALE_AMOUNT = "saleAmount";
    static final String CREATED_AT = "createdAt";
    static final String UPDATED_AT = "updatedAt";

    private LeadAttributes() {
    }
}
