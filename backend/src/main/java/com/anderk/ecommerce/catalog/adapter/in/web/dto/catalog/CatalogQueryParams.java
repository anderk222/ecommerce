package com.anderk.ecommerce.catalog.adapter.in.web.dto.catalog;

import java.util.Locale;

/**
 * The reading of the query parameters that the public and the administration
 * searches share.
 *
 * <p>The API is forgiving on purpose: a parameter that is empty or that does not
 * match any value is treated as "not given", it is not an error.
 */
final class CatalogQueryParams {

    private CatalogQueryParams() {
    }

    /**
     * The constant of {@code type} that matches the text, ignoring case — or the
     * fallback when the text is empty or matches nothing.
     */
    static <E extends Enum<E>> E enumOrElse(Class<E> type, String raw, E fallback) {
        if (raw == null || raw.isBlank()) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }

    /** The page number, 0 when the caller did not send one. */
    static int pageOrFirst(Integer page) {
        return page == null ? 0 : page;
    }
}
