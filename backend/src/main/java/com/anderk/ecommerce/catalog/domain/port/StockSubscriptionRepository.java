package com.anderk.ecommerce.catalog.domain.port;

import java.util.List;
import java.util.Optional;

import com.anderk.ecommerce.catalog.domain.model.StockSubscription;

/**
 * The outbound port of the subscriptions of the stock.
 */
public interface StockSubscriptionRepository {

    /** Inserts or updates a subscription. */
    StockSubscription save(StockSubscription subscription);

    /** The subscription of a lead for an item. */
    Optional<StockSubscription> findByLeadAndItem(Long leadId, Long itemId);

    /** The subscriptions that are still waiting for an item. */
    List<StockSubscription> findPendingByItem(Long itemId);

    /** Marks the subscriptions of an item as notified. */
    void markNotified(List<Long> ids, java.time.Instant when);
}
