package com.anderk.ecommerce.tracking.adapter.in.web;

import com.anderk.ecommerce.tracking.adapter.in.web.dto.TrackEventRequest;
import com.anderk.ecommerce.tracking.application.usecase.TrackActivityUseCase;
import com.anderk.ecommerce.tracking.application.usecase.TrackActivityUseCase.TrackActivityUseCaseCommand;

import jakarta.validation.Valid;

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

    private final TrackActivityUseCase activityTracker;

    public TrackingController(TrackActivityUseCase activityTracker) {
        this.activityTracker = activityTracker;
    }

    /** Records an action of the visitor. */
    @PostMapping
    public ResponseEntity<Void> track(@Valid @RequestBody TrackEventRequest request) {
        
        this.activityTracker.track(
            new TrackActivityUseCaseCommand(request.anonymousId(), request.type(),
                request.entityType(), request.entityId(), request.metadata()));

        return ResponseEntity.accepted().build();
    }
}
