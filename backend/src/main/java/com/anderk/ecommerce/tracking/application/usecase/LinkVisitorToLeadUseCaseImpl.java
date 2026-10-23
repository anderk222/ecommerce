package com.anderk.ecommerce.tracking.application.usecase;

import java.time.Instant;

import com.anderk.ecommerce.tracking.domain.model.Visitor;
import com.anderk.ecommerce.tracking.domain.port.ActivityEventRepository;
import com.anderk.ecommerce.tracking.domain.port.VisitorRepository;

import org.springframework.transaction.annotation.Transactional;


/**
 * The use case that links the activity of a browser with a lead (HU-10).
 */
@Transactional
public class LinkVisitorToLeadUseCaseImpl implements LinkVisitorToLeadUseCase {

    private final VisitorRepository visitorRepository;

    private final ActivityEventRepository eventRepository;

    public LinkVisitorToLeadUseCaseImpl(VisitorRepository visitorRepository,
                             ActivityEventRepository eventRepository) {
        this.visitorRepository = visitorRepository;
        this.eventRepository = eventRepository;
    }

    @Override
    public LinkResult link(LinkVisitorToLeadUseCaseCommand command) {
        if (command.anonymousId() == null || command.anonymousId().isBlank() || command.leadId() == null) {
            return new LinkResult(0);
        }

        this.visitorRepository.findByAnonymousId(command.anonymousId())
                .orElseGet(() -> new Visitor(command.anonymousId(), command.leadId(), Instant.now(), Instant.now()));
        this.visitorRepository.linkToLead(command.anonymousId(), command.leadId());

        int eventsLinked = this.eventRepository.linkToLead(command.anonymousId(), command.leadId());

        return new LinkResult(eventsLinked);
    }
}
