package com.anderk.ecommerce.lead.application.usecases;

import com.anderk.ecommerce.lead.application.command.ChangeLeadStatusCommand;
import com.anderk.ecommerce.lead.application.view.LeadView;

/**
 * The use case that changes the state of a lead (HU-22).
 */
public interface ChangeLeadStatusUseCase {

    /**
     * Changes the state, records the transition and answers the saved lead.
     *
     * <p>The transition is validated by the {@code LeadStatusPolicy}: the
     * backend refuses the ones that are not allowed.
     *
     * @throws com.anderk.ecommerce.domain.error.InvalidStatusTransitionException if the transition
     *         is not allowed
     */
    LeadView changeStatus(Long id, ChangeLeadStatusCommand command);
}
