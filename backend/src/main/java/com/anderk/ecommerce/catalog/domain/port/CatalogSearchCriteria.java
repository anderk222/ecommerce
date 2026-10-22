package com.anderk.ecommerce.catalog.domain.port;

import com.anderk.ecommerce.catalog.domain.model.ItemType;

/**
 * The criteria of a catalog query.
 *
 * <p>One record for the three questions the catalog asks: *what* (the free
 * text and the category), *which kind* (the type) and *how* (the ordering and
 * the filters). The public API and the administration API build the same
 * record; the administration one adds the state filter.
 *
 * @param query the free text, matched against the name and the description
 * @param type the kind of item, null for both
 * @param categorySlug the category, null for all of them
 * @param sort the ordering
 * @param onSale only the items with a discount in force
 * @param lowStockOnly only the items at or below the low stock threshold
 * @param lowStockThreshold the threshold used by {@code lowStockOnly}
 * @param state the lifecycle state filter — the administration only
 */
public record CatalogSearchCriteria(
        String query,
        ItemType type,
        String categorySlug,
        CatalogSort sort,
        boolean onSale,
        boolean lowStockOnly,
        int lowStockThreshold,
        ItemStateFilter state) {

    /** The criteria of the public catalog: active, not deleted, any kind. */
    public static CatalogSearchCriteria publicCatalog(String query, ItemType type, String categorySlug,
                                                       CatalogSort sort, boolean onSale, boolean lowStockOnly,
                                                       int lowStockThreshold) {
        return new CatalogSearchCriteria(query, type, categorySlug, sort, onSale, lowStockOnly,
                lowStockThreshold, ItemStateFilter.ACTIVE);
    }

    /** The criteria of the administration listing. */
    public static CatalogSearchCriteria administration(String query, ItemType type, String categorySlug,
                                                        CatalogSort sort, ItemStateFilter state) {
        return new CatalogSearchCriteria(query, type, categorySlug, sort, false, false,
                com.anderk.ecommerce.catalog.domain.policy.StockPolicy.DEFAULT_LOW_STOCK_THRESHOLD, state);
    }

    /** Whether the query has a free text to match. */
    public boolean hasQuery() {
        return this.query != null && !this.query.isBlank();
    }
}
