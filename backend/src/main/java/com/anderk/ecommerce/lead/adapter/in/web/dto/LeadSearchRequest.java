package com.anderk.ecommerce.lead.adapter.in.web.dto;

import static com.anderk.ecommerce.lead.adapter.in.web.dto.LeadQueryParams.enumOrElse;
import static com.anderk.ecommerce.lead.adapter.in.web.dto.LeadQueryParams.instantOrNull;
import static com.anderk.ecommerce.lead.adapter.in.web.dto.LeadQueryParams.pageOrFirst;

import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.search.LeadSearchCriteria;
import com.anderk.ecommerce.lead.domain.search.LeadSort;

/**
 * The query parameters of the administration listing
 * ({@code GET /api/admin/leads}, HU-21).
 *
 * <p>Every parameter is optional.
 *
 * @param q the free text: the name, the email or the telephone
 * @param status {@code NEW}, {@code IN_PROGRESS} or {@code CLOSED}
 * @param province the province of Ecuador
 * @param source the origin
 * @param from the beginning of the range of dates, ISO-8601
 * @param to the end of the range of dates, ISO-8601
 * @param sort the ordering, {@code NEWEST} by default
 * @param page the 0-based page number
 * @param size the page size
 */
public record LeadSearchRequest(
        String q,
        String status,
        String province,
        String source,
        String from,
        String to,
        String sort,
        Integer page,
        Integer size) {

    public LeadSearchCriteria toCriteria() {
        return LeadSearchCriteria.forAdmin(
                this.q,
                enumOrElse(LeadStatus.class, this.status, null),
                enumOrElse(Province.class, this.province, null),
                enumOrElse(LeadSource.class, this.source, null),
                instantOrNull(this.from),
                instantOrNull(this.to),
                enumOrElse(LeadSort.class, this.sort, LeadSort.DEFAULT));
    }

    public PageRequestDto toPageRequest() {
        return PageRequestDto.of(pageOrFirst(this.page), this.size);
    }
}
