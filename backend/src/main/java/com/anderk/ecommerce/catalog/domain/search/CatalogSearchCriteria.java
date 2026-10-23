package com.anderk.ecommerce.catalog.domain.search;

import com.anderk.ecommerce.catalog.domain.model.ItemType;

/**
 * What a catalog query asks for.
 *
 * <p>Every filter is optional: {@code null} (or {@code false}) means "do not
 * filter by this". The persistence adapter turns only the filters that are
 * present into a query, so adding a new filter never touches the ones that
 * already exist.
 *
 * <p>The public site and the administration build the same record through
 * their own factory: {@link #forPublic} always asks for published items,
 * {@link #forAdmin} lets the caller pick the lifecycle state.
 *
 * @param text free text, matched word by word against the name and the description
 * @param type the kind of item, null for both
 * @param categorySlug the category, null for all of them
 * @param state the lifecycle state, {@link ItemStateFilter#ALL} when not given
 * @param onSaleOnly only the items with a discount in force
 * @param stockAtMost only the tracked items with this many units or fewer, null for no limit
 * @param sort the ordering, {@link CatalogSort#DEFAULT} when not given
 */
public record CatalogSearchCriteria(
        String text,
        ItemType type,
        String categorySlug,
        ItemStateFilter state,
        boolean onSaleOnly,
        Integer stockAtMost,
        CatalogSort sort) {

    public CatalogSearchCriteria {
        text = blankToNull(text);
        categorySlug = blankToNull(categorySlug);
        state = state == null ? ItemStateFilter.ALL : state;
        sort = sort == null ? CatalogSort.DEFAULT : sort;
    }

    /** The criteria of the public catalog: published items only, any kind. */
    public static CatalogSearchCriteria forPublic(String text, ItemType type, String categorySlug,
                                                  boolean onSaleOnly, Integer stockAtMost,
                                                  CatalogSort sort) {
        return new CatalogSearchCriteria(text, type, categorySlug, ItemStateFilter.ACTIVE,
                onSaleOnly, stockAtMost, sort);
    }

    /** The criteria of the administration listing. */
    public static CatalogSearchCriteria forAdmin(String text, ItemType type, String categorySlug,
                                                 ItemStateFilter state, CatalogSort sort) {
        return new CatalogSearchCriteria(text, type, categorySlug, state, false, null, sort);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
