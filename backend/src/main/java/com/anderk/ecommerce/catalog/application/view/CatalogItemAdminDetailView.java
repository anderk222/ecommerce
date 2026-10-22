package com.anderk.ecommerce.catalog.application.view;

import java.math.BigDecimal;
import java.util.List;

import com.anderk.ecommerce.catalog.domain.model.ItemType;

/**
 * The item as the form of the edition reads it.
 *
 * <p>The listing view — {@link CatalogItemAdminView} — is enough for the panel
 * (HU-14) but not for the form (HU-16): it carries no description, no images,
 * no specification table and no category identifiers. This view answers
 * exactly what the form has to put back into its fields.
 *
 * @param id the identifier
 * @param slug the URL fragment
 * @param name the display name
 * @param type the kind of item
 * @param description the long description
 * @param basePrice the price before any discount — the price the form edits
 * @param stock the units available, null when the stock is not tracked
 * @param images the gallery, one URL per entry
 * @param specifications the specification table
 * @param categoryIds the identifiers of the categories the item belongs to
 * @param featured whether the administrator marked it as featured
 */
public record CatalogItemAdminDetailView(
        Long id,
        String slug,
        String name,
        ItemType type,
        String description,
        BigDecimal basePrice,
        Integer stock,
        List<String> images,
        List<SpecificationView> specifications,
        List<Long> categoryIds,
        boolean featured) {

    public CatalogItemAdminDetailView {
        images = images == null ? List.of() : List.copyOf(images);
        specifications = specifications == null ? List.of() : List.copyOf(specifications);
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
    }
}
