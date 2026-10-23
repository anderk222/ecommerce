package com.anderk.ecommerce.catalog.adapter.out.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SearchTextTest {

    @Test
    void normalizeRemovesAccentsCaseAndSymbols() {
        assertThat(SearchText.normalize("  Café   Negro! ")).isEqualTo("cafe negro");
    }

    @Test
    void normalizeOfNullIsEmpty() {
        assertThat(SearchText.normalize(null)).isEmpty();
    }

    @Test
    void ofJoinsNameAndDescription() {
        assertThat(SearchText.of("Café", "Té verde")).isEqualTo("cafe te verde");
        assertThat(SearchText.of("Café", null)).isEqualTo("cafe");
    }

    @Test
    void wordsAreDistinctAndNormalised() {
        assertThat(SearchText.words("Café negro CAFÉ")).containsExactly("cafe", "negro");
    }

    @Test
    void wordsOfOnlySymbolsAreEmpty() {
        // %, _ and friends never reach a LIKE: they are not searchable text
        assertThat(SearchText.words("%_%")).isEmpty();
    }

    @Test
    void wordsAreCutAtEight() {
        assertThat(SearchText.words("a b c d e f g h i j")).hasSize(8);
    }
}
