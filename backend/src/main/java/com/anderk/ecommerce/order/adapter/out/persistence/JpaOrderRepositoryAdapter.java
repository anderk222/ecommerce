package com.anderk.ecommerce.order.adapter.out.persistence;

import java.util.List;
import java.util.Optional;

import com.anderk.ecommerce.order.domain.model.Order;
import com.anderk.ecommerce.order.domain.model.OrderItem;
import com.anderk.ecommerce.order.domain.port.OrderRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;

/**
 * The output adapter of the orders persistence.
 */
@Component
@Transactional
public class JpaOrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository orders;

    public JpaOrderRepositoryAdapter(OrderJpaRepository orders) {
        this.orders = orders;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity();
        entity.setId(order.id());
        entity.setLeadId(order.leadId());
        entity.setAnonymousId(order.anonymousId());
        entity.setTotal(order.total());
        entity.setItemCount(order.itemCount());
        entity.setCreatedAt(order.createdAt());

        List<OrderItemJpaEntity> lines = order.items().stream()
                .map(line -> toEntity(line, entity))
                .toList();
        entity.setItems(new ArrayList<>(lines));

        OrderJpaEntity saved = this.orders.save(entity);
        return toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Order> findById(Long id) {
        return this.orders.findById(id).map(JpaOrderRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> findByLead(Long leadId, int limit) {
        return this.orders.findByLeadIdOrderByCreatedAtDesc(leadId, PageRequest.of(0, limit)).stream()
                .map(JpaOrderRepositoryAdapter::toDomain)
                .toList();
    }

    private static OrderItemJpaEntity toEntity(OrderItem line, OrderJpaEntity order) {
        OrderItemJpaEntity entity = new OrderItemJpaEntity();
        entity.setId(line.id());
        entity.setOrder(order);
        entity.setItemId(line.itemId());
        entity.setItemName(line.itemName());
        entity.setQuantity(line.quantity());
        entity.setUnitPrice(line.unitPrice());
        entity.setLineTotal(line.lineTotal());
        return entity;
    }

    private static Order toDomain(OrderJpaEntity entity) {
        return new Order(
                entity.getId(),
                entity.getLeadId(),
                entity.getAnonymousId(),
                entity.getTotal(),
                entity.getItems().stream()
                        .map(line -> new OrderItem(line.getId(), line.getItemId(), line.getItemName(),
                                line.getQuantity(), line.getUnitPrice(), line.getLineTotal()))
                        .toList(),
                entity.getCreatedAt());
    }
}
