package com.anderk.ecommerce.lead.application.usecases;

import com.anderk.ecommerce.lead.application.command.UpdateLeadCommand;
import com.anderk.ecommerce.lead.application.view.LeadView;

/**
 * The use case that edits the data of a lead (HU-21).
 */
public interface UpdateLeadUseCase {

    /**
     * Edits a lead.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the lead does not exist
     */
    LeadView update(Long id, UpdateLeadCommand command);
}
