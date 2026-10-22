package com.anderk.ecommerce.lead.adapter.out.persistence.status_history;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadStatusHistoryJpaRepository extends JpaRepository<LeadStatusHistoryJpaEntity, Long> {

    List<LeadStatusHistoryJpaEntity> findByLeadIdOrderByChangedAtDesc(Long leadId);
}
