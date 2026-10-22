package com.anderk.ecommerce.lead.domain.port;

import java.util.List;

import com.anderk.ecommerce.lead.domain.model.LeadNote;

/**
 * The outbound port of the notes.
 */
public interface LeadNoteRepository {

    /** Writes a note. */
    LeadNote save(LeadNote note);

    /** The notes of a lead, the most recent first (HU-25). */
    List<LeadNote> findByLead(Long leadId);
}
