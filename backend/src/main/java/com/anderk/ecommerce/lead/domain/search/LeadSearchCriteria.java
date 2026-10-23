package com.anderk.ecommerce.lead.domain.search;

import java.time.Instant;

import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

/**
 * What a search of leads asks for (HU-21).
 *
 * <p>Every filter is optional: {@code null} means "do not filter by this". The
 * persistence adapter turns only the filters that are present into a query, so
 * adding a new filter never touches the ones that already exist.
 *
 * @param query the free text, matched against the name, the email and the phone
 * @param status the state of the pipeline
 * @param province the province of Ecuador
 * @param source the origin
 * @param createdFrom the beginning of the range of dates
 * @param createdTo the end of the range of dates
 * @param sort the ordering, {@link LeadSort#DEFAULT} when not given
 */
public record LeadSearchCriteria(
        String query,
        LeadStatus status,
        Province province,
        LeadSource source,
        Instant createdFrom,
        Instant createdTo,
        LeadSort sort) {

    public LeadSearchCriteria {
        query = blankToNull(query);
        sort = sort == null ? LeadSort.DEFAULT : sort;
    }

    /** The criteria of a search that does not care about the ordering. */
    public LeadSearchCriteria(String query,
                              LeadStatus status,
                              Province province,
                              LeadSource source,
                              Instant createdFrom,
                              Instant createdTo) {
        this(query, status, province, source, createdFrom, createdTo, LeadSort.DEFAULT);
    }

    /** The criteria of the administration listing. */
    public static LeadSearchCriteria forAdmin(String query,
                                              LeadStatus status,
                                              Province province,
                                              LeadSource source,
                                              Instant createdFrom,
                                              Instant createdTo,
                                              LeadSort sort) {
        return new LeadSearchCriteria(query, status, province, source, createdFrom, createdTo, sort);
    }

    /** Whether the search has a free text to match. */
    public boolean hasQuery() {
        return this.query != null;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
