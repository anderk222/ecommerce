package com.anderk.ecommerce.lead.application.usecases;

import java.util.List;

import com.anderk.ecommerce.lead.application.view.LeadNoteView;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.model.LeadNote;

/**
 * Turns the domain into what the CRM answers.
 *
 * <p>Every read and every write of a lead goes through here, so the shape of the
 * answer cannot drift between the listing, the detail and the board.
 */
public class LeadViewMapper {

    public LeadView toView(Lead lead) {
        return new LeadView(
                lead.id(),
                lead.name(),
                lead.email(),
                lead.phone(),
                lead.province(),
                lead.status(),
                List.copyOf(lead.sources()),
                lead.saleAmount(),
                lead.outcome(),
                lead.consent(),
                lead.createdAt(),
                lead.updatedAt());
    }

    public LeadNoteView toView(LeadNote note) {
        return new LeadNoteView(note.id(), note.leadId(), note.body(), note.author(), note.createdAt());
    }
}
