package com.anderk.ecommerce.tracking.application.usecase;

import java.util.Map;

import com.anderk.ecommerce.domain.error.ValidationException;
import com.anderk.ecommerce.tracking.domain.model.EventType;

/**
 * The port that records what the visitor does (HU-09).
 */
public interface TrackActivityUseCase {

    /**
     * Records an action of the visitor.
     *
     * <p>
     * **Best effort and asynchronous: it NEVER throws to the caller.** A
     * visitor who cannot be tracked must still be able to browse.
     *
     * @param anonymousId the identifier of the browser
     * @param eventType   what the visitor did
     * @param entityType  the kind of entity the event is about
     * @param entityId    the identifier of that entity
     * @param metadata    the details of the event
     */
    void track(TrackActivityUseCaseCommand command);

    public record TrackActivityUseCaseCommand(
            String anonymousId, EventType eventType, String entityType,
            Long entityId, Map<String, Object> metadata) {

        public TrackActivityUseCaseCommand {

            if (anonymousId == null || anonymousId.isBlank()) {
                throw new ValidationException("El identificador anónimo es obligatorio");
            }
            if (eventType == null) {
                throw new ValidationException("El tipo del evento es obligatorio");
            }
        }

    }

}
