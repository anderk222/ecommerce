package com.anderk.ecommerce.catalog.application.command;

import java.math.BigDecimal;
import java.util.List;

import com.anderk.ecommerce.catalog.domain.model.ItemType;
import com.anderk.ecommerce.catalog.domain.model.Specification;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The command that creates an item of the catalog.
 *
 * <p>It is validated in the API layer — the domain receives an already valid
 * command.
 *
 * @param type the kind of item
 * @param name the display name
 * @param description the long description
 * @param price the base price
 * @param stock the units available, null when the stock is not tracked
 * @param images the image URLs
 * @param specifications the specification table
 * @param categoryIds the categories the item belongs to
 * @param featured whether the item is featured
 */
public record CreateCatalogItemCommand(
        @NotNull ItemType type,
        @NotBlank String name,
        String description,
        @NotNull @DecimalMin("0.00") BigDecimal price,
        @Min(0) Integer stock,
        List<String> images,
        List<Specification> specifications,
        List<Long> categoryIds,
        boolean featured) {

    public CreateCatalogItemCommand {
        images = images == null ? List.of() : List.copyOf(images);
        specifications = specifications == null ? List.of() : List.copyOf(specifications);
        categoryIds = categoryIds == null ? List.of() : List.copyOf(categoryIds);
        description = description == null ? "" : description;
    }
}
