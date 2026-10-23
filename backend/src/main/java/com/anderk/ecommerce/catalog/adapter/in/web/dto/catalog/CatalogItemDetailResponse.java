package com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.PriceResponse;
import com.anderk.ecommerce.catalog.adapter.in.web.dto.SpecificationResponse;
import java.util.List;

/**
 * An item of the catalog, in its detail page.
 *
 * @param id the identifier
 * @param slug the URL fragment
 * @param name the display name
 * @param type {@code PRODUCT} or {@code SERVICE}
 * @param description the long description
 * @param price the base price and the price the customer pays
 * @param images the gallery
 * @param specifications the specification table
 * @param categories the names of the categories it belongs to
 * @param featured whether the administrator marked it as featured
 * @param available whether it can be added to a cart right now
 * @param stock the units available, null when the stock is not tracked
 * @param stockState what the stock means for this item
 */
public record CatalogItemDetailResponse(
        Long id,
        String slug,
        String name,
        String type,
        String description,
        PriceResponse price,
        List<String> images,
        List<SpecificationResponse> specifications,
        List<String> categories,
        boolean featured,
        boolean available,
        Integer stock,
        String stockState) {
}
