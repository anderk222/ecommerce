package com.anderk.ecommerce.catalog.adapter.in.web.dto;

import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminView;

/**
 * An item of the catalogue, as the administration sees it.
 *
 * @param id the identifier
 * @param slug the URL fragment
 * @param name the display name
 * @param type {@code PRODUCT} or {@code SERVICE}
 * @param price the base price and the price the customer pays
 * @param stock the units available, null when the stock is not tracked
 * @param featured whether the item is featured
 * @param active whether the item is published
 * @param deletedAt when the item was logically deleted
 * @param createdAt when the item was created
 * @param categories the names of the categories it belongs to
 */
public record CatalogItemAdminResponse(
        Long id,
        String slug,
        String name,
        String type,
        PriceResponse price,
        Integer stock,
        boolean featured,
        boolean active,
        Instant deletedAt,
        Instant createdAt,
        List<String> categories) {

    /** The translation of the view of the application. */
    public static CatalogItemAdminResponse of(CatalogItemAdminView view) {
        return new CatalogItemAdminResponse(
                view.id(),
                view.slug(),
                view.name(),
                view.type().name(),
                new PriceResponse(view.price().basePrice(), view.price().finalPrice(),
                        view.price().discountPercentage(), view.price().currency()),
                view.stock(),
                view.featured(),
                view.active(),
                view.deletedAt(),
                view.createdAt(),
                view.categories());
    }
}
