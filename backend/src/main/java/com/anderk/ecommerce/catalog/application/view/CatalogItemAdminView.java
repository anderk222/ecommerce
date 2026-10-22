package com.anderk.ecommerce.catalog.application.view;

import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.domain.model.ItemType;

/**
 * An item of the catalogue, as the administration sees it.
 *
 * <p>It carries the state of the item — active, deleted — and the fields the
 * listing of the panel sorts by (HU-14).
 *
 * @param id the identifier
 * @param slug the URL fragment
 * @param name the display name
 * @param type the kind of item
 * @param price the base price and the price the customer pays
 * @param stock the units available, null when the stock is not tracked
 * @param featured whether the item is featured
 * @param active whether the item is published
 * @param deletedAt when the item was logically deleted
 * @param createdAt when the item was created
 * @param categories the names of the categories it belongs to
 */
public record CatalogItemAdminView(
        Long id,
        String slug,
        String name,
        ItemType type,
        PriceView price,
        Integer stock,
        boolean featured,
        boolean active,
        Instant deletedAt,
        Instant createdAt,
        List<String> categories) {

    public CatalogItemAdminView {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }

    /** Whether the item was logically deleted. */
    public boolean deleted() {
        return this.deletedAt != null;
    }
}
