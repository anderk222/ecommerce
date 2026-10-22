package com.anderk.ecommerce.tracking.adapter.out.persistence.activity;

import java.util.List;

import com.anderk.ecommerce.tracking.domain.model.ActivityEvent;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the activity events.
 */
@Component
@Transactional
public class JpaActivityEventRepositoryAdapter implements ActivityEventRepository {

    private final ActivityEventJpaRepository events;

    public JpaActivityEventRepositoryAdapter(ActivityEventJpaRepository events) {
        this.events = events;
    }

    @Override
    public ActivityEvent save(ActivityEvent event) {
        ActivityEventJpaEntity entity = new ActivityEventJpaEntity();
        entity.setId(event.id());
        entity.setAnonymousId(java.util.UUID.fromString(event.anonymousId()));
        entity.setLeadId(event.leadId());
        entity.setEventType(event.eventType());
        entity.setEntityType(event.entityType());
        entity.setEntityId(event.entityId());
        entity.setMetadata(event.metadata());
        entity.setCreatedAt(event.createdAt());
        return toDomain(this.events.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityEvent> findByLead(Long leadId, int limit) {
        return this.events.findByLeadIdOrderByCreatedAtDesc(leadId, PageRequest.of(0, limit)).stream()
                .map(JpaActivityEventRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ActivityEvent> findByAnonymousId(String anonymousId, int limit) {
        return this.events.findByAnonymousIdOrderByCreatedAtDesc(
                        java.util.UUID.fromString(anonymousId), PageRequest.of(0, limit)).stream()
                .map(JpaActivityEventRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public int linkToLead(String anonymousId, Long leadId) {
        return this.events.linkToLead(java.util.UUID.fromString(anonymousId), leadId);
    }

    private static ActivityEvent toDomain(ActivityEventJpaEntity entity) {
        return new ActivityEvent(
                entity.getId(),
                entity.getAnonymousId().toString(),
                entity.getLeadId(),
                entity.getEventType(),
                entity.getEntityType(),
                entity.getEntityId(),
                entity.getMetadata(),
                entity.getCreatedAt());
    }
}
