package com.anderk.ecommerce.lead.domain.port;

import java.util.List;

import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.lead.domain.model.Lead;

/**
 * The outbound port of the **search** of the leads.
 *
 * <p>It is a port of its own because the search is a different concern from
 * the writes: it reads across the leads, the origins and the ranges of dates.
 */
public interface LeadSearchRepository {

    /** The leads that match the criteria, paginated. */
    DomainPage<Lead> search(LeadSearchCriteria criteria, PageRequestDto page);

    /** Every lead that matches the criteria, without pagination. */
    List<Lead> findAll(LeadSearchCriteria criteria);
}
