package com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.PriceResponse;
import java.util.List;

/**
 * An item of the catalog, in a listing.
 *
 * @param id the identifier
 * @param slug the URL fragment
 * @param name the display name
 * @param type {@code PRODUCT} or {@code SERVICE}
 * @param price the base price and the price the customer pays
 * @param image the first image, for the card of the listing
 * @param featured whether the administrator marked it as featured
 * @param available whether it can be added to a cart right now
 * @param stock the units available, null when the stock is not tracked
 * @param categories the names of the categories it belongs to
 */
public record CatalogItemSummaryResponse(
        Long id,
        String slug,
        String name,
        String type,
        PriceResponse price,
        String image,
        boolean featured,
        boolean available,
        Integer stock,
        List<String> categories) {
}
