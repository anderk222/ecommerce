package com.anderk.ecommerce.lead.application.usecases;

import com.anderk.ecommerce.lead.application.command.CreateLeadCommand;
import com.anderk.ecommerce.lead.application.view.LeadView;

/**
 * The use case that creates a lead by hand (HU-21).
 */
public interface CreateLeadUseCase {

    /**
     * Creates a lead with the origin {@code MANUAL}.
     *
     * @throws com.anderk.ecommerce.domain.error.ConflictException if the email is already taken
     */
    LeadView create(CreateLeadCommand command);
}
