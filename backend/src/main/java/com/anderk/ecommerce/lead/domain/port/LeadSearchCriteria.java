package com.anderk.ecommerce.lead.domain.port;

import java.time.Instant;

import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

/**
 * The criteria of a search of leads (HU-21).
 *
 * @param query the free text, matched against the name, the email and the phone
 * @param status the state of the pipeline
 * @param province the province of Ecuador
 * @param source the origin
 * @param createdFrom the beginning of the range of dates
 * @param createdTo the end of the range of dates
 */
public record LeadSearchCriteria(
        String query,
        LeadStatus status,
        Province province,
        LeadSource source,
        Instant createdFrom,
        Instant createdTo) {

    /** Whether the search has a free text to match. */
    public boolean hasQuery() {
        return this.query != null && !this.query.isBlank();
    }
}
