package com.anderk.ecommerce.catalog.application.view;

import java.math.BigDecimal;

/**
 * The price of an item, as the API answers it.
 *
 * <p>Two prices: the one in the catalogue and the one the customer pays. The
 * difference is the discount in force, calculated by the server at read time
 * (mission §7.6).
 *
 * @param basePrice the price before the discount
 * @param finalPrice the price the customer pays
 * @param discountPercentage the discount in force, 0 when there is none
 * @param currency the currency of both prices
 */
public record PriceView(BigDecimal basePrice, BigDecimal finalPrice, int discountPercentage, String currency) {

    public boolean discounted() {
        return this.discountPercentage > 0;
    }
}
