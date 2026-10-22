package com.anderk.ecommerce.catalog.adapter.in.listener;

import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.catalog.domain.event.StockReplenishedEvent;
import com.anderk.ecommerce.catalog.domain.model.StockNotification;
import com.anderk.ecommerce.catalog.domain.model.StockSubscription;
import com.anderk.ecommerce.catalog.domain.port.StockNotificationSender;
import com.anderk.ecommerce.catalog.domain.port.StockSubscriptionRepository;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Notifies the visitors who asked for an item that came back (HU-08).
 *
 * <p>It listens to the event of the catalogue. The catalogue does not know
 * that this listener exists.
 */
@Component
public class StockSubscriptionNotifier {

    private static final Logger log = LoggerFactory.getLogger(StockSubscriptionNotifier.class);

    private final StockSubscriptionRepository subscriptionRepository;

    private final StockNotificationSender notificationSender;

    private final LeadRepository leadRepository;

    public StockSubscriptionNotifier(StockSubscriptionRepository subscriptionRepository,
                                     StockNotificationSender notificationSender,
                                     LeadRepository leadRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.notificationSender = notificationSender;
        this.leadRepository = leadRepository;
    }

    /** Notifies the pending subscriptions of the item that was replenished. */
    @EventListener
    @Transactional
    public void onStockReplenished(StockReplenishedEvent event) {
        List<StockSubscription> pending = this.subscriptionRepository.findPendingByItem(event.itemId());
        if (pending.isEmpty()) {
            return;
        }

        Instant now = Instant.now();
        for (StockSubscription subscription : pending) {
            this.leadRepository.findById(subscription.leadId()).ifPresent(lead ->
                    this.notificationSender.send(new StockNotification(
                            lead.id(), lead.email(), event.itemId(), nameOf(event.itemId()))));
        }

        this.subscriptionRepository.markNotified(
                pending.stream().map(StockSubscription::id).toList(), now);
        log.info("{} suscripciones notificadas por el surtido del ítem {}", pending.size(), event.itemId());
    }

    private String nameOf(Long itemId) {
        return "el producto " + itemId;
    }
}
