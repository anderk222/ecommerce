package com.anderk.ecommerce.order.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * A purchase (HU-07).
 *
 * <p>The purchase is simulated: there is no payment gateway, no money moves.
 * What exists is the order, the stock that is discounted, and the lead that
 * the customer becomes.
 *
 * @param id the identifier
 * @param leadId the lead the purchase belongs to
 * @param anonymousId the browser of the customer
 * @param total the total of the order
 * @param items the lines of the order
 * @param createdAt when the purchase happened
 */
public record Order(
        Long id,
        Long leadId,
        UUID anonymousId,
        BigDecimal total,
        List<OrderItem> items,
        Instant createdAt) {

    public Order {
        items = items == null ? List.of() : List.copyOf(items);
        total = total == null ? BigDecimal.ZERO : total;
    }

    /** How many units the order holds. */
    public int itemCount() {
        return this.items.stream().mapToInt(OrderItem::quantity).sum();
    }
}
