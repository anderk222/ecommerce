package com.anderk.ecommerce.lead.domain.port;

import java.util.List;

import com.anderk.ecommerce.lead.domain.model.LeadStatusHistory;

/**
 * The outbound port of the history of the states.
 */
public interface LeadStatusHistoryRepository {

    /** Records a change of state. */
    LeadStatusHistory save(LeadStatusHistory history);

    /** The changes of state of a lead, the most recent first (HU-25). */
    List<LeadStatusHistory> findByLead(Long leadId);
}
