package com.anderk.ecommerce.catalog.adapter.out.persistence.catalog;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.anderk.ecommerce.catalog.domain.search.CatalogSort;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class CatalogItemSortingTest {

    private static List<String> orders(CatalogSort sort) {
        return CatalogItemSorting.of(sort).stream()
                .map((Sort.Order order) -> order.getProperty() + " " + order.getDirection())
                .toList();
    }

    @Test
    void newestIsCreationDateDescending() {
        assertThat(orders(CatalogSort.NEWEST)).containsExactly("createdAt DESC", "id DESC");
    }

    @Test
    void oldestIsCreationDateAscending() {
        assertThat(orders(CatalogSort.OLDEST)).containsExactly("createdAt ASC", "id ASC");
    }

    @Test
    void priceTiesAreBrokenByNameAndThenById() {
        assertThat(orders(CatalogSort.PRICE_ASC))
                .containsExactly("price ASC", "name ASC", "id ASC");
        assertThat(orders(CatalogSort.PRICE_DESC))
                .containsExactly("price DESC", "name ASC", "id DESC");
    }

    @Test
    void nameSortsAlphabeticallyInBothDirections() {
        assertThat(orders(CatalogSort.NAME_ASC)).containsExactly("name ASC", "id ASC");
        assertThat(orders(CatalogSort.NAME_DESC)).containsExactly("name DESC", "id DESC");
    }

    @Test
    void bestSellingTiesAreBrokenByNewest() {
        assertThat(orders(CatalogSort.BEST_SELLING))
                .containsExactly("salesCount DESC", "createdAt DESC", "id DESC");
    }

    @Test
    void everySortEndsWithTheIdentifier() {
        for (CatalogSort sort : CatalogSort.values()) {
            List<String> orders = orders(sort);
            assertThat(orders.get(orders.size() - 1)).startsWith("id ");
        }
    }
}
