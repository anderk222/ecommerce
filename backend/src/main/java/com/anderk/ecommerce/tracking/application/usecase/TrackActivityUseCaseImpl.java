package com.anderk.ecommerce.tracking.application.usecase;

import java.time.Instant;
import java.util.Map;

import com.anderk.ecommerce.tracking.domain.model.ActivityEvent;
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
@Transactional(propagation = Propagation.REQUIRES_NEW)
public class TrackActivityUseCaseImpl implements TrackActivityUseCase {

    private static final Logger log = LoggerFactory.getLogger(TrackActivityUseCaseImpl.class);

    private final ActivityEventRepository eventRepository;

    private final VisitorRepository visitorRepository;

    public TrackActivityUseCaseImpl(ActivityEventRepository eventRepository,
                               VisitorRepository visitorRepository) {
        this.eventRepository = eventRepository;
        this.visitorRepository = visitorRepository;
    }

    @Override
    public void track(TrackActivityUseCaseCommand command ) {
        try {
            Visitor visitor = this.visitorRepository.findByAnonymousId(command.anonymousId())
                    .orElseGet(() -> this.visitorRepository.save(
                            new Visitor(command.anonymousId(), null, Instant.now(), Instant.now())));
            this.visitorRepository.save(
                    new Visitor(visitor.anonymousId(), visitor.leadId(), visitor.createdAt(), Instant.now()));

            this.eventRepository.save(new ActivityEvent(
                    null, command.anonymousId(), visitor.leadId(), command.eventType(), command.entityType(), command.entityId(),
                    command.metadata() == null ? Map.of() :command.metadata(), Instant.now()));
        } catch (RuntimeException exception) {
            log.warn("The activity could not be recorded: {}", exception.getMessage());
        }
    }
}
