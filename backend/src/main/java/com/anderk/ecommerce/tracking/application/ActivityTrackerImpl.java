package com.anderk.ecommerce.tracking.application;

import java.time.Instant;
import java.util.Map;

import com.anderk.ecommerce.shared.annotation.UseCase;
import com.anderk.ecommerce.tracking.domain.model.ActivityEvent;
import com.anderk.ecommerce.tracking.domain.model.EventType;
import com.anderk.ecommerce.tracking.domain.model.Visitor;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;
import com.anderk.ecommerce.tracking.domain.port.VisitorRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that records what the visitor does.
 *
 * <p>It never throws: a failure to track is logged and forgotten. The
 * visitor's journey matters more than the record of it (HU-09).
 */
@UseCase
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class ActivityTrackerImpl implements ActivityTracker {

    private static final Logger log = LoggerFactory.getLogger(ActivityTrackerImpl.class);

    private final ActivityEventRepository eventRepository;

    private final VisitorRepository visitorRepository;

    public ActivityTrackerImpl(ActivityEventRepository eventRepository,
                               VisitorRepository visitorRepository) {
        this.eventRepository = eventRepository;
        this.visitorRepository = visitorRepository;
    }

    @Override
    public void track(String anonymousId, EventType eventType, String entityType, Long entityId,
                      Map<String, Object> metadata) {
        try {
            Visitor visitor = this.visitorRepository.findByAnonymousId(anonymousId)
                    .orElseGet(() -> this.visitorRepository.save(
                            new Visitor(anonymousId, null, Instant.now(), Instant.now())));
            this.visitorRepository.save(
                    new Visitor(visitor.anonymousId(), visitor.leadId(), visitor.createdAt(), Instant.now()));

            this.eventRepository.save(new ActivityEvent(
                    null, anonymousId, visitor.leadId(), eventType, entityType, entityId,
                    metadata == null ? Map.of() : metadata, Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("The activity could not be recorded: {}", exception.getMessage());
        }
    }
}
