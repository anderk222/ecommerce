package com.anderk.ecommerce.catalog.application.port;

/**
 * The port of the simulated channel of notifications.
 *
 * <p>It lives in the application layer of the catalogue because it is the
 * *application* that writes to it, not the persistence of another feature.
 */
public interface NotificationRepository {

    /**
     * Writes a notification.
     *
     * @param recipient who receives it
     * @param subject the subject
     * @param body the body
     */
    void record(String recipient, String subject, String body);
}
