package com.anderk.ecommerce.catalog.application.view;

import java.util.List;

import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.model.StockState;

/**
 * The item as it appears in its detail page.
 *
 * @param id the identifier
 * @param slug the URL fragment
 * @param name the display name
 * @param type the kind of item
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
public record CatalogItemDetail(
        Long id,
        String slug,
        String name,
        ItemType type,
        String description,
        PriceView price,
        List<String> images,
        List<SpecificationView> specifications,
        List<String> categories,
        boolean featured,
        boolean available,
        Integer stock,
        StockState stockState) {

    public CatalogItemDetail {
        images = images == null ? List.of() : List.copyOf(images);
        specifications = specifications == null ? List.of() : List.copyOf(specifications);
        categories = categories == null ? List.of() : List.copyOf(categories);
    }
}
