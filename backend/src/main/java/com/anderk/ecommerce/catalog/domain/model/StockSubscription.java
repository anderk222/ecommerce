package com.anderk.ecommerce.catalog.domain.model;

import java.time.Instant;

/**
 * A visitor who asked to be notified when an item comes back (HU-08).
 *
 * <p>There is one subscription per lead and item. Once it is notified, it
 * keeps the stamp of the notification.
 *
 * @param id the identifier
 * @param leadId the lead that asked
 * @param itemId the item that is out of stock
 * @param notifiedAt when the visitor was notified, null while it is pending
 * @param createdAt when the visitor asked
 */
public record StockSubscription(Long id, Long leadId, Long itemId, Instant notifiedAt, Instant createdAt) {

    /** Whether the subscription is still waiting. */
    public boolean pending() {
        return this.notifiedAt == null;
    }
}
