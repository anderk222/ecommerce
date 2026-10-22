package com.anderk.ecommerce.catalog.domain.policy;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

import com.anderk.ecommerce.catalog.domain.model.Discount;

/**
 * The only pricing rule of the system: a percentage off the base price.
 *
 * <p>HU-19 fixes the discount as a percentage between 0 and 100. Adding a
 * second kind of discount means adding another implementation of
 * {@link PricingPolicy}, not changing this one (tech-stack section 2.2.11,
 * *Open/Closed*).
 */
public class PercentagePricingPolicy implements PricingPolicy {

    private static final int SCALE = 2;

    @Override
    public PriceBreakdown calculate(BigDecimal basePrice, Discount discount, Instant now) {
        if (basePrice == null) {
            throw new IllegalArgumentException("El precio base no puede ser nulo");
        }
        if (discount == null || !discount.isEffectiveAt(now)) {
            return PriceBreakdown.noDiscount(basePrice);
        }

        BigDecimal factor = BigDecimal.valueOf(100 - discount.percentage())
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        BigDecimal finalPrice = basePrice.multiply(factor).setScale(SCALE, RoundingMode.HALF_UP);

        return new PriceBreakdown(basePrice.setScale(SCALE, RoundingMode.HALF_UP), finalPrice,
                discount.percentage(), true);
    }
}
