package com.anderk.ecommerce.catalog.adapter.in.web.dto;

import java.util.List;

import com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog.CatalogItemSummaryResponse;

/**
 * The content of the home page.
 *
 * @param featured the items the administrator marked as featured: the ones of
 *        the carousel
 * @param services the most recent published services
 * @param products the most recent published products
 * @param bestSelling the items with the most confirmed purchases
 * @param onSale the items with a discount in force
 */
public record HomeResponse(
        List<CatalogItemSummaryResponse> featured,
        List<CatalogItemSummaryResponse> services,
        List<CatalogItemSummaryResponse> products,
        List<CatalogItemSummaryResponse> bestSelling,
        List<CatalogItemSummaryResponse> onSale) {
}
