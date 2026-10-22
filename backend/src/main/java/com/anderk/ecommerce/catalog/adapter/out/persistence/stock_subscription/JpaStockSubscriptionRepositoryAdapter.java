package com.anderk.ecommerce.catalog.adapter.out.persistence.stock_subscription;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.anderk.ecommerce.catalog.domain.model.StockSubscription;
import com.anderk.ecommerce.catalog.domain.port.StockSubscriptionRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the subscriptions of the stock.
 */
@Component
@Transactional
public class JpaStockSubscriptionRepositoryAdapter implements StockSubscriptionRepository {

    private final StockSubscriptionJpaRepository subscriptions;

    public JpaStockSubscriptionRepositoryAdapter(StockSubscriptionJpaRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public StockSubscription save(StockSubscription subscription) {
        StockSubscriptionJpaEntity entity = subscription.id() == null
                ? new StockSubscriptionJpaEntity()
                : this.subscriptions.findById(subscription.id()).orElseGet(StockSubscriptionJpaEntity::new);
        entity.setId(subscription.id());
        entity.setLeadId(subscription.leadId());
        entity.setItemId(subscription.itemId());
        entity.setNotifiedAt(subscription.notifiedAt());
        entity.setCreatedAt(subscription.createdAt() == null ? Instant.now() : subscription.createdAt());
        return toDomain(this.subscriptions.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StockSubscription> findByLeadAndItem(Long leadId, Long itemId) {
        return this.subscriptions.findByLeadIdAndItemId(leadId, itemId)
                .map(JpaStockSubscriptionRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StockSubscription> findPendingByItem(Long itemId) {
        return this.subscriptions.findByItemIdAndNotifiedAtIsNull(itemId).stream()
                .map(JpaStockSubscriptionRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public void markNotified(List<Long> ids, Instant when) {
        this.subscriptions.findAllById(ids).forEach(entity -> {
            entity.setNotifiedAt(when);
            this.subscriptions.save(entity);
        });
    }

    private static StockSubscription toDomain(StockSubscriptionJpaEntity entity) {
        return new StockSubscription(entity.getId(), entity.getLeadId(), entity.getItemId(),
                entity.getNotifiedAt(), entity.getCreatedAt());
    }
}
