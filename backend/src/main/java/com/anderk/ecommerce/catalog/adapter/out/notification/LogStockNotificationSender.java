package com.anderk.ecommerce.catalog.adapter.out.notification;

import com.anderk.ecommerce.catalog.application.port.NotificationRepository;
import com.anderk.ecommerce.catalog.domain.model.StockNotification;
import com.anderk.ecommerce.catalog.domain.port.StockNotificationSender;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * The simulated channel of notifications (HU-08).
 *
 * <p>There is no real email: the notice is written in the log and in the
 * table {@code notifications}. A real channel would be another implementation
 * of {@link StockNotificationSender}, and nothing else would change.
 */
@Component
public class LogStockNotificationSender implements StockNotificationSender {

    private static final Logger log = LoggerFactory.getLogger(LogStockNotificationSender.class);

    private final NotificationRepository notificationRepository;

    public LogStockNotificationSender(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void send(StockNotification notification) {
        String subject = "«" + notification.itemName() + "» vuelve a estar disponible";
        String body = "Hola,\n\nEl producto «" + notification.itemName()
                + "» que pediste avisar ya tiene stock.\n\n— Xtrim";

        log.info("Notificación de stock para {}: {}", notification.email(), subject);
        this.notificationRepository.record(notification.email(), subject, body);
    }
}
