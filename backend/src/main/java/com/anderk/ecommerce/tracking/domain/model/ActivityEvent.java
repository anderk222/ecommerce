package com.anderk.ecommerce.tracking.domain.model;

import java.time.Instant;
import java.util.Map;

import com.anderk.ecommerce.domain.error.ValidationException;

/**
 * One action of the visitor (HU-09).
 *
 * <p>Every event carries the anonymous identifier of the visitor and, once the
 * visitor has given a contact, the lead it belongs to.
 *
 * @param id the identifier, null before it is persisted
 * @param anonymousId the identifier of the browser
 * @param leadId the lead the event belongs to, null while the visitor is anonymous
 * @param eventType what the visitor did
 * @param entityType the kind of entity the event is about
 * @param entityId the identifier of that entity
 * @param metadata the details of the event
 * @param createdAt when it happened
 */
public record ActivityEvent(
        Long id,
        String anonymousId,
        Long leadId,
        EventType eventType,
        String entityType,
        Long entityId,
        Map<String, Object> metadata,
        Instant createdAt) {

    public ActivityEvent {
        if (anonymousId == null || anonymousId.isBlank()) {
            throw new ValidationException("El identificador anónimo es obligatorio");
        }
        if (eventType == null) {
            throw new ValidationException("El tipo del evento es obligatorio");
        }
        metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
    }
}
