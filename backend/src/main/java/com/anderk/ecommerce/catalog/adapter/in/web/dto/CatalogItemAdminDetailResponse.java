package com.anderk.ecommerce.catalog.adapter.in.web.dto;

import java.math.BigDecimal;
import java.util.List;

import com.anderk.ecommerce.catalog.application.view.CatalogItemAdminDetailView;

/**
 * The item the administration reads back for its edition.
 *
 * @param id the identifier
 * @param slug the URL fragment
 * @param name the display name
 * @param type {@code PRODUCT} or {@code SERVICE}
 * @param description the long description
 * @param basePrice the price before any discount — the price the form edits
 * @param stock the units available, null when the stock is not tracked
 * @param images the gallery, one URL per entry
 * @param specifications the specification table
 * @param categoryIds the identifiers of the categories the item belongs to
 * @param featured whether the administrator marked it as featured
 */
public record CatalogItemAdminDetailResponse(
        Long id,
        String slug,
        String name,
        String type,
        String description,
        BigDecimal basePrice,
        Integer stock,
        List<String> images,
        List<SpecificationResponse> specifications,
        List<Long> categoryIds,
        boolean featured) {

    /** The translation of the view of the application. */
    public static CatalogItemAdminDetailResponse of(CatalogItemAdminDetailView view) {
        return new CatalogItemAdminDetailResponse(
                view.id(),
                view.slug(),
                view.name(),
                view.type().name(),
                view.description(),
                view.basePrice(),
                view.stock(),
                view.images(),
                view.specifications().stream()
                        .map(specification -> new SpecificationResponse(
                                specification.name(), specification.value()))
                        .toList(),
                view.categoryIds(),
                view.featured());
    }
}
