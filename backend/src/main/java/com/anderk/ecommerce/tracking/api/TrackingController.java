package com.anderk.ecommerce.tracking.api;

import java.util.Map;

import com.anderk.ecommerce.tracking.application.ActivityTracker;
import com.anderk.ecommerce.tracking.domain.model.EventType;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The endpoint that records the activity of the visitor (HU-09).
 *
 * <p>It answers **202**: the event is accepted, and it is recorded. The
 * visitor is not kept waiting for the record.
 */
@RestController
@RequestMapping("/api/public/events")
public class TrackingController {

    private final ActivityTracker activityTracker;

    public TrackingController(ActivityTracker activityTracker) {
        this.activityTracker = activityTracker;
    }

    /** Records an action of the visitor. */
    @PostMapping
    public ResponseEntity<Void> track(@Valid @RequestBody TrackEventRequest request) {
        this.activityTracker.track(
                request.anonymousId(),
                request.type(),
                request.entityType(),
                request.entityId(),
                request.metadata());
        return ResponseEntity.accepted().build();
    }

    /**
     * The body of the event.
     *
     * @param anonymousId the identifier of the browser
     * @param type what the visitor did
     * @param entityType the kind of entity the event is about
     * @param entityId the identifier of that entity
     * @param metadata the details of the event
     */
    public record TrackEventRequest(
            @NotBlank String anonymousId,
            @NotNull EventType type,
            String entityType,
            Long entityId,
            Map<String, Object> metadata) {
    }
}
