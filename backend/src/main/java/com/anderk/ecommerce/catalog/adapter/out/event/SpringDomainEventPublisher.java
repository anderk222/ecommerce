package com.anderk.ecommerce.catalog.adapter.out.event;

import com.anderk.ecommerce.catalog.domain.port.DomainEventPublisher;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * The adapter that publishes the events of the domain with the events of
 * Spring.
 */
@Component
public class SpringDomainEventPublisher implements DomainEventPublisher {

    private final ApplicationEventPublisher publisher;

    public SpringDomainEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    @Override
    public void publish(Object event) {
        this.publisher.publishEvent(event);
    }
}
