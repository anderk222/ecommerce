package com.anderk.ecommerce.lead.application.usecases;

import com.anderk.ecommerce.lead.application.command.AddLeadNoteCommand;
import com.anderk.ecommerce.lead.application.view.LeadNoteView;

/**
 * The use case that writes a note about a lead (HU-25).
 */
public interface AddLeadNoteUseCase {

    /**
     * Writes a note.
     *
     * @throws com.anderk.ecommerce.domain.error.ValidationException if the text is empty
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the lead does not exist
     */
    LeadNoteView addNote(Long id, AddLeadNoteCommand command);
}
