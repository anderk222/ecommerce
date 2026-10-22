package com.anderk.ecommerce.catalog.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.anderk.ecommerce.catalog.domain.vo.Slug;
import com.anderk.ecommerce.domain.error.ValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class SlugTest {

    @Test
    @DisplayName("accents, case and punctuation are normalised away")
    void accentsCaseAndPunctuationAreNormalisedAway() {
        assertThat(Slug.of("Ropa de Invierno — ¡Oferta!").value()).isEqualTo("ropa-de-invierno-oferta");
    }

    @Test
    @DisplayName("a slug is always lower case, without accents")
    void aSlugIsAlwaysLowerCaseWithoutAccents() {
        assertThat(Slug.of("Café").value()).isEqualTo("cafe");
        assertThat(Slug.of("Ñ").value()).isEqualTo("n");
    }

    @Test
    @DisplayName("the surrounding dashes are trimmed")
    void theSurroundingDashesAreTrimmed() {
        assertThat(Slug.of("  --- Servicio de Internet ---  ").value()).isEqualTo("servicio-de-internet");
    }

    @Test
    @DisplayName("a text that normalises to nothing is rejected")
    void aTextThatNormalisesToNothingIsRejected() {
        assertThatThrownBy(() -> Slug.of("!!!")).isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("a slug that is not normalised is rejected")
    void aSlugThatIsNotNormalisedIsRejected() {
        assertThatThrownBy(() -> new Slug("Con Espacios")).isInstanceOf(ValidationException.class);
    }
}
