package com.anderk.ecommerce.catalog.adapter.in.web.dto;

import java.math.BigDecimal;

/**
 * The price of an item, as the API answers it.
 *
 * @param basePrice the price before the discount
 * @param finalPrice the price the customer pays
 * @param discountPercentage the discount in force, 0 when there is none
 * @param currency the currency of both prices
 */
public record PriceResponse(BigDecimal basePrice, BigDecimal finalPrice, int discountPercentage, String currency) {
}
