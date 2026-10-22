package com.anderk.ecommerce.lead.application;

import com.anderk.ecommerce.lead.application.view.KanbanBoard;
import com.anderk.ecommerce.lead.application.view.LeadDetailView;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.port.LeadSearchCriteria;
import com.anderk.ecommerce.shared.pagination.PageRequestDto;
import com.anderk.ecommerce.shared.pagination.PageResponse;

/**
 * The inbound port of the **reads** of the leads (HU-21, HU-23, HU-25).
 */
public interface LeadQueryService {

    /** The leads that match the criteria, paginated. */
    PageResponse<LeadView> search(LeadSearchCriteria criteria, PageRequestDto page);

    /**
     * The detail of a lead: its data, its timeline, its orders and its notes.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the lead does not exist
     */
    LeadDetailView getDetail(Long id);

    /** The board of the pipeline: one column per state, with the counts and the totals. */
    KanbanBoard getKanban(LeadSearchCriteria criteria);
}
