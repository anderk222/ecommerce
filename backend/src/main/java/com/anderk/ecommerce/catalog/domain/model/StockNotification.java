package com.anderk.ecommerce.catalog.domain.model;

/**
 * The notice that an item is back in stock (HU-08).
 *
 * @param leadId the lead to notify
 * @param email the address of the lead
 * @param itemId the item that is back
 * @param itemName the name of the item
 */
public record StockNotification(Long leadId, String email, Long itemId, String itemName) {
}
