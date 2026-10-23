package com.anderk.ecommerce.lead.application;

import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.domain.pagination.PageResponse;
import com.anderk.ecommerce.lead.application.usecases.result.KanbanBoardResult;
import com.anderk.ecommerce.lead.application.usecases.result.LeadDetailResult;
import com.anderk.ecommerce.lead.application.usecases.result.LeadResult;
import com.anderk.ecommerce.lead.domain.port.LeadSearchCriteria;

/**
 * The inbound port of the **reads** of the leads (HU-21, HU-23, HU-25).
 */
public interface LeadQueryService {

    /** The leads that match the criteria, paginated. */
    PageResponse<LeadResult> search(LeadSearchCriteria criteria, PageRequestDto page);

    /**
     * The detail of a lead: its data, its timeline, its orders and its notes.
     *
     * @throws com.anderk.ecommerce.domain.error.NotFoundException if the lead does not exist
     */
    LeadDetailResult getDetail(Long id);

    /** The board of the pipeline: one column per state, with the counts and the totals. */
    KanbanBoardResult getKanban(LeadSearchCriteria criteria);
}
