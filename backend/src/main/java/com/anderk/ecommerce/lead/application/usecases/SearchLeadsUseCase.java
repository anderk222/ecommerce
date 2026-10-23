package com.anderk.ecommerce.lead.application.usecases;

import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.domain.pagination.PageResponse;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.search.LeadSearchCriteria;

/**
 * The use case behind the listing of the leads (HU-21).
 */
public interface SearchLeadsUseCase {

    /** The leads that match the criteria, paginated and ordered. */
    PageResponse<LeadView> search(LeadSearchCriteria criteria, PageRequestDto page);
}
