package com.anderk.ecommerce.tracking.adapter.out.persistence.visitor;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The persistence of a browser of the registry.
 */
@Entity
@Table(name = "visitors")
public class VisitorJpaEntity {

    @Id
    @Column(name = "anonymous_id", columnDefinition = "uuid")
    private UUID anonymousId;

    @Column(name = "lead_id")
    private Long leadId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;

    public UUID getAnonymousId() {
        return this.anonymousId;
    }

    public void setAnonymousId(UUID anonymousId) {
        this.anonymousId = anonymousId;
    }

    public Long getLeadId() {
        return this.leadId;
    }

    public void setLeadId(Long leadId) {
        this.leadId = leadId;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getLastSeenAt() {
        return this.lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }
}
