package com.anderk.ecommerce.lead.adapter.out.persistence.status_history;

import java.time.Instant;

import com.anderk.ecommerce.lead.domain.model.LeadStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The persistence of a change of the state of a lead.
 */
@Entity
@Table(name = "lead_status_history")
public class LeadStatusHistoryJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "lead_id", nullable = false)
    private Long leadId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 32)
    private LeadStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 32)
    private LeadStatus newStatus;

    @Column(name = "changed_by", nullable = false)
    private String changedBy;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getLeadId() {
        return this.leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public LeadStatus getPreviousStatus() {
        return this.previousStatus;
    }

    public void setPreviousStatus(LeadStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public LeadStatus getNewStatus() {
        return this.newStatus;
    }

    public void setNewStatus(LeadStatus newStatus) {
        this.newStatus = newStatus;
    }

    public String getChangedBy() {
        return this.changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public Instant getChangedAt() {
        return this.changedAt;
    }

    public void setChangedAt(Instant changedAt) {
        this.changedAt = changedAt;
    }
}
