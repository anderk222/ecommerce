package com.anderk.ecommerce.lead.application.usecases.result;

import java.time.Instant;

/**
 * A note of the team, as the CRM answers it.
 *
 * @param id the identifier
 * @param leadId the lead the note is about
 * @param body the text of the note
 * @param author the member of the staff who wrote it
 * @param createdAt when it was written
 */
public record LeadNoteResult(Long id, Long leadId, String body, String author, Instant createdAt) {
}
