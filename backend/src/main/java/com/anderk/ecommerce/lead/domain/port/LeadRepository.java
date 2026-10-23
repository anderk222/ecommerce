package com.anderk.ecommerce.lead.domain.port;

import java.util.List;
import java.util.Optional;

import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.search.LeadSearchCriteria;

/**
 * The outbound port of the lead aggregate: its reads *and* its writes.
 *
 * <p>One port per aggregate (tech-stack section 2.2.3). The read/write split
 * lives one level up, in the use cases, where the callers really do differ.
 */
public interface LeadRepository {

    /** Inserts or updates a lead. */
    Lead save(Lead lead);

    /** The lead with that identifier. */
    Optional<Lead> findById(Long id);

    /** The lead with that email — the identity of a lead. */
    Optional<Lead> findByEmail(String email);

    /** The leads that match the criteria, paginated and ordered. */
    DomainPage<Lead> search(LeadSearchCriteria criteria, PageRequestDto page);

    /** Every lead that matches the criteria, ordered, without pagination. */
    List<Lead> findAll(LeadSearchCriteria criteria);

    /** Removes a lead. */
    void deleteById(Long id);
}
