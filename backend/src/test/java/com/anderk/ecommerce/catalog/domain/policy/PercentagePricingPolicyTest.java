package com.anderk.ecommerce.catalog.domain.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;

import com.anderk.ecommerce.catalog.domain.model.Discount;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * The pricing rule, exercised without Spring.
 *
 * <p>The roadmap's condition for P03 is exactly this: the policy is unit
 * tested with no container, no database and no clock.
 */
class PercentagePricingPolicyTest {

    private static final Instant NOW = Instant.parse("2026-10-20T09:30:00Z");

    private final PercentagePricingPolicy policy = new PercentagePricingPolicy();

    @Test
    @DisplayName("without a discount the price does not move")
    void withoutDiscountThePriceDoesNotMove() {
        PriceBreakdown breakdown = this.policy.calculate(new BigDecimal("100.00"), null, NOW);

        assertThat(breakdown.basePrice()).isEqualByComparingTo("100.00");
        assertThat(breakdown.finalPrice()).isEqualByComparingTo("100.00");
        assertThat(breakdown.discountPercentage()).isZero();
        assertThat(breakdown.discountApplied()).isFalse();
    }

    @Test
    @DisplayName("a discount in force takes the percentage off")
    void discountInForceTakesThePercentageOff() {
        Discount discount = new Discount(1L, 10L, 20, NOW.minusSeconds(3600), null, null);

        PriceBreakdown breakdown = this.policy.calculate(new BigDecimal("100.00"), discount, NOW);

        assertThat(breakdown.finalPrice()).isEqualByComparingTo("80.00");
        assertThat(breakdown.discountPercentage()).isEqualTo(20);
        assertThat(breakdown.discountApplied()).isTrue();
    }

    @Test
    @DisplayName("a revoked discount does not apply")
    void revokedDiscountDoesNotApply() {
        Discount discount = new Discount(1L, 10L, 20, NOW.minusSeconds(3600), null, NOW.minusSeconds(60));

        PriceBreakdown breakdown = this.policy.calculate(new BigDecimal("100.00"), discount, NOW);

        assertThat(breakdown.finalPrice()).isEqualByComparingTo("100.00");
        assertThat(breakdown.discountApplied()).isFalse();
    }

    @Test
    @DisplayName("a discount that has not started yet does not apply")
    void discountThatHasNotStartedDoesNotApply() {
        Discount discount = new Discount(1L, 10L, 50, NOW.plusSeconds(3600), null, null);

        PriceBreakdown breakdown = this.policy.calculate(new BigDecimal("100.00"), discount, NOW);

        assertThat(breakdown.finalPrice()).isEqualByComparingTo("100.00");
        assertThat(breakdown.discountApplied()).isFalse();
    }

    @Test
    @DisplayName("a discount that has ended does not apply")
    void discountThatHasEndedDoesNotApply() {
        Discount discount = new Discount(1L, 10L, 50, NOW.minusSeconds(7200), NOW.minusSeconds(3600), null);

        PriceBreakdown breakdown = this.policy.calculate(new BigDecimal("100.00"), discount, NOW);

        assertThat(breakdown.finalPrice()).isEqualByComparingTo("100.00");
        assertThat(breakdown.discountApplied()).isFalse();
    }

    @Test
    @DisplayName("the price is rounded to two decimals")
    void thePriceIsRoundedToTwoDecimals() {
        Discount discount = new Discount(1L, 10L, 15, NOW.minusSeconds(60), null, null);

        PriceBreakdown breakdown = this.policy.calculate(new BigDecimal("99.99"), discount, NOW);

        assertThat(breakdown.finalPrice()).isEqualByComparingTo("84.99");
        assertThat(breakdown.finalPrice().scale()).isLessThanOrEqualTo(2);
    }

    @Test
    @DisplayName("a 100% discount gives a final price of zero, never a negative one")
    void fullDiscountGivesZero() {
        Discount discount = new Discount(1L, 10L, 100, NOW.minusSeconds(60), null, null);

        PriceBreakdown breakdown = this.policy.calculate(new BigDecimal("99.99"), discount, NOW);

        assertThat(breakdown.finalPrice()).isEqualByComparingTo("0.00");
        assertThat(breakdown.finalPrice().signum()).isNotNegative();
    }

    @Test
    @DisplayName("a discount outside 0..100 is rejected")
    void discountOutsideTheRangeIsRejected() {
        assertThatThrownBy(() -> new Discount(1L, 10L, 120, null, null, null))
                .isInstanceOf(com.anderk.ecommerce.domain.error.ValidationException.class);
    }
}
