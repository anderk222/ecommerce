package com.anderk.ecommerce.catalog.application.view;

import java.util.List;

import com.anderk.ecommerce.catalog.domain.model.ItemType;

/**
 * The item as it appears in a listing.
 *
 * @param id the identifier
 * @param slug the URL fragment
 * @param name the display name
 * @param type the kind of item
 * @param price the base price and the price the customer pays
 * @param image the first image, for the card of the listing
 * @param featured whether the administrator marked it as featured
 * @param available whether it can be added to a cart right now
 * @param stock the units available, null when the stock is not tracked
 * @param categories the names of the categories it belongs to
 */
public record CatalogItemSummary(
        Long id,
        String slug,
        String name,
        ItemType type,
        PriceView price,
        String image,
        boolean featured,
        boolean available,
        Integer stock,
        List<String> categories) {

    public CatalogItemSummary {
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
