package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadJpaRepository extends JpaRepository<LeadJpaEntity, Long> {

    Optional<LeadJpaEntity> findByEmail(String email);
}
