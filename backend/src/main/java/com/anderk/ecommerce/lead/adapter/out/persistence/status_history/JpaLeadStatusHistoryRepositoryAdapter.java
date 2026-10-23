package com.anderk.ecommerce.lead.adapter.out.persistence.status_history;

import java.util.List;

import com.anderk.ecommerce.lead.domain.model.LeadStatusHistory;
import com.anderk.ecommerce.lead.domain.port.LeadStatusHistoryRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the history of the states.
 */
@Transactional
public class JpaLeadStatusHistoryRepositoryAdapter implements LeadStatusHistoryRepository {

    private final LeadStatusHistoryJpaRepository history;

    public JpaLeadStatusHistoryRepositoryAdapter(LeadStatusHistoryJpaRepository history) {
        this.history = history;
    }

    @Override
    public LeadStatusHistory save(LeadStatusHistory entry) {
        LeadStatusHistoryJpaEntity entity = new LeadStatusHistoryJpaEntity();
        entity.setId(entry.id());
        entity.setLeadId(entry.leadId());
        entity.setPreviousStatus(entry.previousStatus());
        entity.setNewStatus(entry.newStatus());
        entity.setChangedBy(entry.changedBy());
        entity.setChangedAt(entry.changedAt());
        return toDomain(this.history.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<LeadStatusHistory> findByLead(Long leadId) {
        return this.history.findByLeadIdOrderByChangedAtDesc(leadId).stream()
                .map(JpaLeadStatusHistoryRepositoryAdapter::toDomain)
                .toList();
    }

    private static LeadStatusHistory toDomain(LeadStatusHistoryJpaEntity entity) {
        return new LeadStatusHistory(entity.getId(), entity.getLeadId(), entity.getPreviousStatus(),
                entity.getNewStatus(), entity.getChangedBy(), entity.getChangedAt());
    }
}
