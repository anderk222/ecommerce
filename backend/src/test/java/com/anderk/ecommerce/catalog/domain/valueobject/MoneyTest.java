package com.anderk.ecommerce.catalog.domain.valueobject;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;

import com.anderk.ecommerce.catalog.domain.vo.Money;
import com.anderk.ecommerce.domain.error.ValidationException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    @DisplayName("the amount always carries two decimals")
    void theAmountAlwaysCarriesTwoDecimals() {
        assertThat(Money.of(new BigDecimal("10")).amount()).isEqualByComparingTo("10.00");
    }

    @Test
    @DisplayName("a negative amount is rejected")
    void aNegativeAmountIsRejected() {
        assertThatThrownBy(() -> Money.of(new BigDecimal("-1")))
                .isInstanceOf(ValidationException.class);
    }

    @Test
    @DisplayName("a discount is taken off the amount")
    void aDiscountIsTakenOffTheAmount() {
        assertThat(Money.of(new BigDecimal("80.00")).discountedBy(20).amount()).isEqualByComparingTo("64.00");
    }

    @Test
    @DisplayName("two currencies are never added together")
    void twoCurrenciesAreNeverAddedTogether() {
        Money dollars = Money.of(new BigDecimal("10"), "USD");
        Money euros = Money.of(new BigDecimal("10"), "EUR");

        assertThatThrownBy(() -> dollars.plus(euros)).isInstanceOf(ValidationException.class);
    }
}
