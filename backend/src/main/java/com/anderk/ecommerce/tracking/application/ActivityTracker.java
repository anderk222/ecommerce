package com.anderk.ecommerce.tracking.application;

import java.util.Map;

import com.anderk.ecommerce.tracking.domain.model.EventType;

/**
 * The port that records what the visitor does (HU-09).
 */
public interface ActivityTracker {

    /**
     * Records an action of the visitor.
     *
     * <p>**Best effort and asynchronous: it NEVER throws to the caller.** A
     * visitor who cannot be tracked must still be able to browse.
     *
     * @param anonymousId the identifier of the browser
     * @param eventType what the visitor did
     * @param entityType the kind of entity the event is about
     * @param entityId the identifier of that entity
     * @param metadata the details of the event
     */
    void track(String anonymousId, EventType eventType, String entityType, Long entityId,
               Map<String, Object> metadata);
}
