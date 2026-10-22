package com.anderk.ecommerce.catalog.domain.port;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.anderk.ecommerce.catalog.domain.model.Discount;

/**
 * The outbound port of the discount aggregate.
 */
public interface DiscountRepository {

    /** Inserts or updates a discount. */
    Discount save(Discount discount);

    Optional<Discount> findById(Long id);

    /** The discount of an item that is in force at the given instant, if any. */
    Optional<Discount> findEffectiveByItem(Long itemId, Instant now);

    /** Every discount of an item, the current one first. */
    List<Discount> findByItem(Long itemId);

    /** The discount that applies to an item at the given instant. */
    default Optional<Discount> effectiveDiscountFor(Long itemId, Instant now) {
        return findEffectiveByItem(itemId, now);
    }

    /**
     * The discounts in force for several items at once, keyed by item.
     *
     * <p>A listing asks for a whole page: one query for the page is better
     * than one query per item.
     */
    Map<Long, Discount> findEffectiveByItems(Collection<Long> itemIds, Instant now);
}
