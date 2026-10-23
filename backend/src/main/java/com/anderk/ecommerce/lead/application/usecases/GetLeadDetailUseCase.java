package com.anderk.ecommerce.lead.application.usecases;

import com.anderk.ecommerce.lead.application.view.LeadDetailView;

/**
 * The use case that reads the detail of a lead (HU-25).
 */
public interface GetLeadDetailUseCase {

    /**
     * The data, the timeline, the orders, the notes and the history of states.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the lead does not exist
     */
    LeadDetailView getDetail(Long id);
}
