package com.anderk.ecommerce.catalog.adapter.out.persistence.notification;

import java.time.Instant;

import com.anderk.ecommerce.catalog.application.port.NotificationRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The simulated channel of notifications (HU-08).
 *
 * <p>There is no real email: the notice is written in the table
 * {@code notifications} and in the log.
 */
@Component
public class JpaNotificationRepositoryAdapter implements NotificationRepository {

    private final NotificationJpaRepository notifications;

    public JpaNotificationRepositoryAdapter(NotificationJpaRepository notifications) {
        this.notifications = notifications;
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(String recipient, String subject, String body) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setChannel("LOG");
        entity.setRecipient(recipient);
        entity.setSubject(subject);
        entity.setBody(body);
        entity.setCreatedAt(Instant.now());
        this.notifications.save(entity);
    }
}
