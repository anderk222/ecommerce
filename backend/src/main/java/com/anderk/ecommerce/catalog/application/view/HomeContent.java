package com.anderk.ecommerce.catalog.application.view;

import java.util.List;

/**
 * The content of the home page.
 *
 * <p>HU-01: the featured items, the most bought ones and the ones with an
 * offer in force. The two sections of the catalogue —the services and the
 * products— travel apart, so the home can show the services first.
 *
 * @param featured the items the administrator marked as featured: the ones the
 *        shop wants to sell, and the ones of the carousel
 * @param services the most recent published services
 * @param products the most recent published products
 * @param bestSelling the items with the most confirmed purchases
 * @param onSale the items with a discount in force
 */
public record HomeContent(List<CatalogItemSummary> featured,
                           List<CatalogItemSummary> services,
                           List<CatalogItemSummary> products,
                           List<CatalogItemSummary> bestSelling,
                           List<CatalogItemSummary> onSale) {

    public HomeContent {
        featured = featured == null ? List.of() : List.copyOf(featured);
        services = services == null ? List.of() : List.copyOf(services);
        products = products == null ? List.of() : List.copyOf(products);
        bestSelling = bestSelling == null ? List.of() : List.copyOf(bestSelling);
        onSale = onSale == null ? List.of() : List.copyOf(onSale);
    }
}
