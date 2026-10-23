package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.anderk.ecommerce.lead.domain.search.LeadSort;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class LeadSortingTest {

    private static List<String> orders(LeadSort sort) {
        return LeadSorting.of(sort).stream()
                .map((Sort.Order order) -> order.getProperty() + " " + order.getDirection())
                .toList();
    }

    @Test
    void defaultSortIsNewest() {
        assertThat(orders(null)).containsExactly("createdAt DESC", "id DESC");
    }

    @Test
    void newestIsCreationDateDescending() {
        assertThat(orders(LeadSort.NEWEST)).containsExactly("createdAt DESC", "id DESC");
    }

    @Test
    void oldestIsCreationDateAscending() {
        assertThat(orders(LeadSort.OLDEST)).containsExactly("createdAt ASC", "id ASC");
    }

    @Test
    void nameSortsAlphabeticallyInBothDirections() {
        assertThat(orders(LeadSort.NAME_ASC)).containsExactly("name ASC", "id ASC");
        assertThat(orders(LeadSort.NAME_DESC)).containsExactly("name DESC", "id DESC");
    }

    @Test
    void saleAmountTiesAreBrokenByNameAndThenById() {
        assertThat(orders(LeadSort.SALE_AMOUNT_DESC))
                .containsExactly("saleAmount DESC", "name ASC", "id DESC");
    }

    @Test
    void updatedSortIsMostRecentFirst() {
        assertThat(orders(LeadSort.UPDATED_DESC)).containsExactly("updatedAt DESC", "id DESC");
    }

    @Test
    void everySortEndsWithTheIdentifier() {
        for (LeadSort sort : LeadSort.values()) {
            List<String> orders = orders(sort);
            assertThat(orders.get(orders.size() - 1)).startsWith("id ");
        }
    }
}
