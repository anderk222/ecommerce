package com.anderk.ecommerce.order.domain.port;

import java.util.List;
import java.util.Optional;

import com.anderk.ecommerce.order.domain.model.Order;

/**
 * The outbound port of the orders.
 */
public interface OrderRepository {

    /** Inserts an order with its lines. */
    Order save(Order order);

    /** The order with that identifier. */
    Optional<Order> findById(Long id);

    /** The orders of a lead, the most recent first (HU-25). */
    List<Order> findByLead(Long leadId, int limit);
}
