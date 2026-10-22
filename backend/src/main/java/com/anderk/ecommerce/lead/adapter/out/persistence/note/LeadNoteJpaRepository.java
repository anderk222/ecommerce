package com.anderk.ecommerce.lead.adapter.out.persistence.note;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface LeadNoteJpaRepository extends JpaRepository<LeadNoteJpaEntity, Long> {

    List<LeadNoteJpaEntity> findByLeadIdOrderByCreatedAtDesc(Long leadId);
}
