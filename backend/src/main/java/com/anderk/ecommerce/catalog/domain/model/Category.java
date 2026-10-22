package com.anderk.ecommerce.catalog.domain.model;

import com.anderk.ecommerce.catalog.domain.vo.Slug;

/**
 * A category of the unified catalog.
 *
 * @param id the identifier, null before it is persisted
 * @param name the display name
 * @param slug the URL fragment, unique across the catalog
 */
public record Category(Long id, String name, Slug slug) {

    public Category {
        if (name == null || name.isBlank()) {
            throw new com.anderk.ecommerce.domain.error.ValidationException("El nombre de la categoría es obligatorio");
        }
        if (slug == null) {
            throw new com.anderk.ecommerce.domain.error.ValidationException("El slug de la categoría es obligatorio");
        }
    }
}
