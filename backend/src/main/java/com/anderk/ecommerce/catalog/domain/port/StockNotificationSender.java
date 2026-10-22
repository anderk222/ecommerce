package com.anderk.ecommerce.catalog.domain.port;

import com.anderk.ecommerce.catalog.domain.model.StockNotification;

/**
 * The port of the channel that notifies the visitors (HU-08).
 *
 * <p>There is no real email: the channel is simulated. A new channel is a
 * new implementation, and nothing else changes (tech-stack §2.2.11).
 */
public interface StockNotificationSender {

    /**
     * Sends the notice.
     *
     * <p>**Idempotent per subscription**: sending the same notice twice does
     * not send it twice.
     */
    void send(StockNotification notification);
}
