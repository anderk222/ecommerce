package com.anderk.ecommerce.tracking.application;

import java.time.Instant;

import com.anderk.ecommerce.shared.annotation.UseCase;
import com.anderk.ecommerce.tracking.domain.model.Visitor;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;
import com.anderk.ecommerce.tracking.domain.port.VisitorRepository;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case that links the activity of a browser with a lead (HU-10).
 */
@UseCase
@Transactional
public class VisitorLinkerImpl implements VisitorLinker {

    private final VisitorRepository visitorRepository;

    private final ActivityEventRepository eventRepository;

    public VisitorLinkerImpl(VisitorRepository visitorRepository,
                             ActivityEventRepository eventRepository) {
        this.visitorRepository = visitorRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public int link(String anonymousId, Long leadId) {
        if (anonymousId == null || anonymousId.isBlank() || leadId == null) {
            return 0;
        }

        this.visitorRepository.findByAnonymousId(anonymousId)
                .orElseGet(() -> new Visitor(anonymousId, leadId, Instant.now(), Instant.now()));
        this.visitorRepository.linkToLead(anonymousId, leadId);

        return this.eventRepository.linkToLead(anonymousId, leadId);
    }
}
