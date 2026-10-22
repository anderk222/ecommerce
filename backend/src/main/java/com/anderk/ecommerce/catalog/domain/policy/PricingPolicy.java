package com.anderk.ecommerce.catalog.domain.policy;

import java.math.BigDecimal;
import java.time.Instant;

import com.anderk.ecommerce.catalog.domain.model.Discount;

/**
 * The rule that turns a base price and a discount into the price the customer
 * pays.
 *
 * <p>It is the pure core of the catalog: no I/O, no clock, no framework. The
 * "now" is always a parameter, so the rule is testable and deterministic.
 *
 * <p>Mission section 7.6: *the backend owns the price*. The client never
 * sends a price; it asks and the server answers.
 */
public interface PricingPolicy {

    /**
     * Calculates the price of an item at a given instant.
     *
     * <p>Pure: no I/O. Never returns null and {@code finalPrice >= 0}. A
     * discount applies when it was not revoked and the instant falls inside
     * its window (a null end means indefinite). The discount may be null.
     *
     * @param basePrice the price before the discount
     * @param discount the discount in force, or null
     * @param now the instant the price is calculated for
     */
    PriceBreakdown calculate(BigDecimal basePrice, Discount discount, Instant now);
}
