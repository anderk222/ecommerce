package com.anderk.ecommerce.lead.application.usecases;

import java.util.List;

import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.domain.pagination.PageResponse;
import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.search.LeadSearchCriteria;

import org.springframework.transaction.annotation.Transactional;

/**
 * The use case behind the listing of the leads (HU-21).
 */
@Transactional(readOnly = true)
public class SearchLeadsUseCaseImpl implements SearchLeadsUseCase {

    private final LeadRepository leadRepository;

    private final LeadViewMapper viewMapper;

    public SearchLeadsUseCaseImpl(LeadRepository leadRepository, LeadViewMapper viewMapper) {
        this.leadRepository = leadRepository;
        this.viewMapper = viewMapper;
    }

    @Override
    public PageResponse<LeadView> search(LeadSearchCriteria criteria, PageRequestDto page) {
        DomainPage<Lead> found = this.leadRepository.search(criteria, page);
        List<LeadView> content = found.content().stream().map(this.viewMapper::toView).toList();
        return PageResponse.of(content, found.page(), found.size(), found.totalElements());
    }
}
