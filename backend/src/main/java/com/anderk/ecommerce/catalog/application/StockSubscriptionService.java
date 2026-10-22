package com.anderk.ecommerce.catalog.application;

import java.util.List;

/**
 * The port of the subscriptions of the stock (HU-08).
 */
public interface StockSubscriptionService {

    /**
     * Subscribes a visitor to the notice that an item comes back.
     *
     * <p>It creates the lead — with the origin `STOCK_NOTIFY` — and the
     * subscription. There is one subscription per lead and item.
     */
    void subscribe(SubscribeCommand command);

    /**
     * The subscription the visitor asks for.
     *
     * @param email the email of the visitor
     * @param name the name of the visitor
     * @param province the province of Ecuador
     * @param consent whether the visitor agreed to be contacted
     * @param anonymousId the browser of the visitor
     * @param itemId the item that is out of stock
     */
    record SubscribeCommand(
            String email,
            String name,
            String province,
            boolean consent,
            String anonymousId,
            Long itemId,
            List<Long> itemIds) {
    }
}
