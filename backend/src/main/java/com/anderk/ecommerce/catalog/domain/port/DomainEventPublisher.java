package com.anderk.ecommerce.catalog.domain.port;

/**
 * The outbound port that publishes the events of the domain.
 *
 * <p>The domain announces what happened; the infrastructure decides how it
 * travels.
 */
public interface DomainEventPublisher {

    /** Publishes an event. */
    void publish(Object event);
}
