package com.anderk.ecommerce.catalog.domain.policy;

import java.math.BigDecimal;

/**
 * The result of a price calculation.
 *
 * @param basePrice the price before the discount
 * @param finalPrice the price the customer pays
 * @param discountPercentage the discount that was applied, 0 when there is none
 * @param discountApplied whether a discount actually changed the price
 */
public record PriceBreakdown(BigDecimal basePrice, BigDecimal finalPrice, int discountPercentage,
                              boolean discountApplied) {

    public static PriceBreakdown noDiscount(BigDecimal basePrice) {
        return new PriceBreakdown(basePrice, basePrice, 0, false);
    }
}
