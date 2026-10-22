package com.anderk.ecommerce.tracking.adapter.out.persistence.visitor;

import java.time.Instant;
import java.util.Optional;

import com.anderk.ecommerce.tracking.domain.model.Visitor;
import com.anderk.ecommerce.tracking.domain.port.VisitorRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the registry of the browsers.
 */
@Component
@Transactional
public class JpaVisitorRepositoryAdapter implements VisitorRepository {

    private final VisitorJpaRepository visitors;

    public JpaVisitorRepositoryAdapter(VisitorJpaRepository visitors) {
        this.visitors = visitors;
    }

    @Override
    public Visitor save(Visitor visitor) {
        VisitorJpaEntity entity = this.visitors.findById(java.util.UUID.fromString(visitor.anonymousId()))
                .orElseGet(VisitorJpaEntity::new);
        entity.setAnonymousId(java.util.UUID.fromString(visitor.anonymousId()));
        entity.setLeadId(visitor.leadId());
        entity.setCreatedAt(visitor.createdAt() == null ? Instant.now() : visitor.createdAt());
        entity.setLastSeenAt(visitor.lastSeenAt() == null ? Instant.now() : visitor.lastSeenAt());
        return toDomain(this.visitors.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Visitor> findByAnonymousId(String anonymousId) {
        return this.visitors.findById(java.util.UUID.fromString(anonymousId))
                .map(JpaVisitorRepositoryAdapter::toDomain);
    }

    @Override
    public void linkToLead(String anonymousId, Long leadId) {
        this.visitors.findById(java.util.UUID.fromString(anonymousId)).ifPresent(entity -> {
            entity.setLeadId(leadId);
            this.visitors.save(entity);
        });
    }

    private static Visitor toDomain(VisitorJpaEntity entity) {
        return new Visitor(entity.getAnonymousId().toString(), entity.getLeadId(),
                entity.getCreatedAt(), entity.getLastSeenAt());
    }
}
