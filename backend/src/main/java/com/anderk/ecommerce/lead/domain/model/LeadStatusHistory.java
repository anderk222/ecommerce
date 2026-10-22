package com.anderk.ecommerce.lead.domain.model;

import java.time.Instant;

/**
 * A change of the state of a lead (HU-22).
 *
 * @param id the identifier
 * @param leadId the lead whose state changed
 * @param previousStatus the state before
 * @param newStatus the state after
 * @param changedBy the member of the staff who changed it
 * @param changedAt when it changed
 */
public record LeadStatusHistory(
        Long id,
        Long leadId,
        LeadStatus previousStatus,
        LeadStatus newStatus,
        String changedBy,
        Instant changedAt) {
}
