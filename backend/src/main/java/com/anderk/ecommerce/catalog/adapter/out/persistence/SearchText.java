package com.anderk.ecommerce.catalog.adapter.out.persistence;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * The normal form of the text the public search matches against (HU-02).
 *
 * <p>The same rule is used twice: when an item is saved, to fill its
 * {@code search_text} column, and when somebody searches, to normalise what
 * they typed. That is what makes "cafe" find "Café" without a PostgreSQL
 * extension.
 *
 * <p>The normal form keeps only {@code a-z}, {@code 0-9} and single spaces, so
 * it can never contain a {@code LIKE} wildcard ({@code %} or {@code _}).
 */
public final class SearchText {

    /** A longer search is cut here: every word is one more condition in the query. */
    private static final int MAX_WORDS = 8;

    private SearchText() {
    }

    /** Lower case, without accents, with every other character turned into a space. */
    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return Normalizer.normalize(raw, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", " ")
                .trim();
    }

    /** The text that is stored for an item: its name and its description, normalised. */
    public static String of(String name, String description) {
        return normalize((name == null ? "" : name) + " " + (description == null ? "" : description));
    }

    /** The distinct words of a search, normalised. Empty when nothing searchable was typed. */
    public static List<String> words(String raw) {
        String normalized = normalize(raw);
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(normalized.split(" "))
                .distinct()
                .limit(MAX_WORDS)
                .toList();
    }
}
