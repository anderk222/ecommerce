package com.anderk.ecommerce.order.adapter.out.persistence;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, Long> {

    List<OrderJpaEntity> findByLeadIdOrderByCreatedAtDesc(Long leadId, org.springframework.data.domain.Pageable pageable);
}
