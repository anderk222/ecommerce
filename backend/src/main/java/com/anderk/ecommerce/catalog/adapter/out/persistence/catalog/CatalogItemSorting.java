package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.CREATED_AT;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.ID;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.NAME;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.PRICE;
import static com.anderk.ecommerce.catalog.adapter.out.persistence.catalog.CatalogItemAttributes.SALES_COUNT;

import java.util.ArrayList;
import java.util.List;

import com.anderk.ecommerce.catalog.domain.search.CatalogSort;

import org.springframework.data.domain.Sort;

/**
 * Turns a domain {@link CatalogSort} into the {@link Sort} that the database
 * understands. It is the only place that knows both vocabularies.
 *
 * <p>An ordering is made of three parts, always in this order:
 * <ol>
 *   <li>the field the caller asked for, in the direction they asked for;
 *   <li>a second field for the cases where the first one often repeats (equal
 *       prices, equal sales);
 *   <li>the identifier, in the same direction as the first field. Without it
 *       two rows that tie could swap places between page 1 and page 2, and an
 *       item would show up twice or never.
 * </ol>
 */
public final class CatalogItemSorting {

    private CatalogItemSorting() {
    }

    public static Sort of(CatalogSort sort) {
        Sort.Direction direction = sort.direction() == CatalogSort.Direction.ASC
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        List<Sort.Order> orders = new ArrayList<>();
        orders.add(new Sort.Order(direction, attributeOf(sort.field())));
        orders.addAll(thenBy(sort.field()));
        orders.add(new Sort.Order(direction, ID));
        return Sort.by(orders);
    }

    private static String attributeOf(CatalogSort.Field field) {
        return switch (field) {
            case CREATED_AT -> CREATED_AT;
            case PRICE -> PRICE;
            case NAME -> NAME;
            case SALES -> SALES_COUNT;
        };
    }

    /** The second ordering of the fields that are likely to tie. */
    private static List<Sort.Order> thenBy(CatalogSort.Field field) {
        return switch (field) {
            case PRICE -> List.of(Sort.Order.asc(NAME));
            case SALES -> List.of(Sort.Order.desc(CREATED_AT));
            case CREATED_AT, NAME -> List.of();
        };
    }
}
