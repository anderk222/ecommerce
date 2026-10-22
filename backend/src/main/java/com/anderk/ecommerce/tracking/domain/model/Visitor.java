package com.anderk.ecommerce.tracking.domain.model;

import java.time.Instant;

/**
 * A browser that has been seen (HU-10).
 *
 * <p>The registry is what makes the history survive: when the visitor gives
 * a contact, the link between the browser and the lead is written here, and
 * the events that arrive later already carry the lead.
 *
 * @param anonymousId the identifier of the browser
 * @param leadId the lead the browser belongs to, null while it is anonymous
 * @param createdAt when the browser was first seen
 * @param lastSeenAt when it was last seen
 */
public record Visitor(String anonymousId, Long leadId, Instant createdAt, Instant lastSeenAt) {
}
