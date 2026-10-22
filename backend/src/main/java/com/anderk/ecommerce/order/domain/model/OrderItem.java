package com.anderk.ecommerce.order.domain.model;

import java.math.BigDecimal;

/**
 * One line of an order (HU-07).
 *
 * <p>The prices are the ones that were in force **at the moment of the
 * purchase**: the order is the history, and it does not change when the price
 * of the catalogue changes.
 *
 * @param id the identifier
 * @param itemId the item that was bought
 * @param itemName the name of the item at the moment of the purchase
 * @param quantity how many units
 * @param unitPrice the price of one unit
 * @param lineTotal the price of the line
 */
public record OrderItem(
        Long id,
        Long itemId,
        String itemName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal lineTotal) {

    public OrderItem {
        if (quantity <= 0) {
            throw new IllegalArgumentException("La cantidad debe ser mayor que cero");
        }
    }
}
