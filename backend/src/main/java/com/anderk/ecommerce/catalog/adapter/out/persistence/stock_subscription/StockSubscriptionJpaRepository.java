package com.anderk.ecommerce.catalog.adapter.out.persistence.stock_subscription;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface StockSubscriptionJpaRepository extends JpaRepository<StockSubscriptionJpaEntity, Long> {

    Optional<StockSubscriptionJpaEntity> findByLeadIdAndItemId(Long leadId, Long itemId);

    List<StockSubscriptionJpaEntity> findByItemIdAndNotifiedAtIsNull(Long itemId);
}
