package com.anderk.ecommerce.lead.application.view;

import java.time.Instant;

/**
 * A note of the team, as the CRM answers it (HU-25).
 *
 * @param id the identifier
 * @param leadId the lead the note is about
 * @param body the text of the note
 * @param author the member of the staff who wrote it
 * @param createdAt when it was written
 */
public record LeadNoteView(Long id, Long leadId, String body, String author, Instant createdAt) {
}
