package com.anderk.ecommerce.catalog.adapter.out.persistence.discount;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.anderk.ecommerce.catalog.domain.model.Discount;
import com.anderk.ecommerce.catalog.domain.port.DiscountRepository;

import org.springframework.transaction.annotation.Transactional;

@Transactional
public class JpaDiscountRepositoryAdapter implements DiscountRepository {

    private final DiscountJpaRepository discounts;

    public JpaDiscountRepositoryAdapter(DiscountJpaRepository discounts) {
        this.discounts = discounts;
    }

    @Override
    public Discount save(Discount discount) {
        DiscountJpaEntity entity = discount.id() == null
                ? new DiscountJpaEntity()
                : this.discounts.findById(discount.id()).orElseGet(DiscountJpaEntity::new);
        entity.setId(discount.id());
        entity.setItemId(discount.itemId());
        entity.setPercentage(discount.percentage());
        entity.setStartsAt(discount.startsAt());
        entity.setEndsAt(discount.endsAt());
        entity.setRevokedAt(discount.revokedAt());
        return toDomain(this.discounts.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Discount> findById(Long id) {
        return this.discounts.findById(id).map(JpaDiscountRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Discount> findEffectiveByItem(Long itemId, Instant now) {
        return this.discounts.listEffectiveByItem(itemId, now).stream().findFirst()
                .map(JpaDiscountRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Discount> findByItem(Long itemId) {
        return this.discounts.listByItem(itemId).stream()
                .map(JpaDiscountRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<Long, Discount> findEffectiveByItems(Collection<Long> itemIds, Instant now) {
        Map<Long, Discount> byItem = new LinkedHashMap<>();
        if (itemIds == null || itemIds.isEmpty()) {
            return byItem;
        }
        for (DiscountJpaEntity entity : this.discounts.listEffectiveByItems(new java.util.ArrayList<>(itemIds), now)) {
            byItem.putIfAbsent(entity.getItemId(), toDomain(entity));
        }
        return byItem;
    }

    private static Discount toDomain(DiscountJpaEntity entity) {
        return new Discount(entity.getId(), entity.getItemId(), entity.getPercentage(),
                entity.getStartsAt(), entity.getEndsAt(), entity.getRevokedAt());
    }
}
