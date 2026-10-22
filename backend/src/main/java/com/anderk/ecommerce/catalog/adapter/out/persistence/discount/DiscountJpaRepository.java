package com.anderk.ecommerce.catalog.adapter.out.persistence.discount;

import java.time.Instant;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DiscountJpaRepository extends JpaRepository<DiscountJpaEntity, Long> {

    /** The discount of an item that is in force at the given instant, if any. */
    @Query("""
            select d from DiscountJpaEntity d
            where d.itemId = :itemId
              and d.revokedAt is null
              and (d.startsAt is null or d.startsAt <= :now)
              and (d.endsAt is null or d.endsAt >= :now)
            order by d.startsAt desc, d.id desc
            """)
    List<DiscountJpaEntity> listEffectiveByItem(@Param("itemId") Long itemId, @Param("now") Instant now);

    /** The discounts in force for several items at once. */
    @Query("""
            select d from DiscountJpaEntity d
            where d.itemId in :itemIds
              and d.revokedAt is null
              and (d.startsAt is null or d.startsAt <= :now)
              and (d.endsAt is null or d.endsAt >= :now)
            order by d.id desc
            """)
    List<DiscountJpaEntity> listEffectiveByItems(@Param("itemIds") List<Long> itemIds, @Param("now") Instant now);

    /** Every discount of an item, the current one first. */
    @Query("select d from DiscountJpaEntity d where d.itemId = :itemId order by d.id desc")
    List<DiscountJpaEntity> listByItem(@Param("itemId") Long itemId);
}
