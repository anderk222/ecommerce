package com.anderk.ecommerce.catalog.domain.vo;

import java.text.Normalizer;
import java.util.Locale;

import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.ValidationException;

/**
 * The URL fragment of an item or a category.
 *
 * <p>HU-26 fixes the shape: lower case, no accents, no special characters.
 * The normalisation lives here, in the domain, so the item detail page and
 * the sitemap agree on what a slug looks like without duplicating the rule.
 *
 * @param value the normalised slug
 */
public record Slug(String value) {

    public Slug {
        if (value == null || value.isBlank()) {
            throw new ValidationException("El slug no puede estar vacío");
        }
        if (!value.equals(normalize(value))) {
            throw new ValidationException("El slug no está normalizado: " + value,
                    ErrorDetail.of("slug", "debe estar en minúsculas, sin tildes ni caracteres especiales"));
        }
    }

    /** Normalises any text into a slug: "Ropa para Invierno" -> "ropa-para-invierno". */
    public static Slug of(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new ValidationException("No se puede generar un slug a partir de un texto vacío");
        }
        return new Slug(normalize(raw));
    }

    /** The normalisation itself, without the guard of the constructor. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        String withoutAccents = Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return withoutAccents.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-+", "")
                .replaceAll("-+$", "");
    }

    @Override
    public String toString() {
        return this.value;
    }
}
