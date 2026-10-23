package com.anderk.ecommerce.tracking.adapter.in.web.dto;

import java.util.Map;

import com.anderk.ecommerce.tracking.domain.model.EventType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * The body of the event.
 *
 * @param anonymousId the identifier of the browser
 * @param type        what the visitor did
 * @param entityType  the kind of entity the event is about
 * @param entityId    the identifier of that entity
 * @param metadata    the details of the event
 */
public record TrackEventRequest(
        @NotBlank String anonymousId,
        @NotNull EventType type,
        String entityType,
        Long entityId,
        Map<String, Object> metadata) {
}
