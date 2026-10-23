package com.anderk.ecommerce.catalog.application.command;

import java.util.List;

/**
 * The command that subscribes a visitor to the notice that an item comes
 * back (HU-08).
 *
 * @param email the email of the visitor
 * @param name the name of the visitor
 * @param province the province of Ecuador
 * @param consent whether the visitor agreed to be contacted
 * @param anonymousId the browser of the visitor
 * @param itemId the item that is out of stock
 * @param itemIds the items, when there is more than one
 */
public record SubscribeToStockCommand(
        String email,
        String name,
        String province,
        boolean consent,
        String anonymousId,
        Long itemId,
        List<Long> itemIds) {
}
