package com.anderk.ecommerce.lead.adapter.in.web.dto;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * The reading of the query parameters of the listing of the leads.
 *
 * <p>The API is forgiving on purpose: a parameter that is empty or that does not
 * match any value is treated as "not given", it is not an error.
 */
final class LeadQueryParams {

    private LeadQueryParams() {
    }

    /**
     * The constant of an enum that matches the text, ignoring case — or the
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

    /** The instant in ISO-8601, or null when it is empty or malformed. */
    static Instant instantOrNull(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Instant.parse(raw.trim());
        } catch (DateTimeParseException exception) {
            return null;
        }
    }
}
