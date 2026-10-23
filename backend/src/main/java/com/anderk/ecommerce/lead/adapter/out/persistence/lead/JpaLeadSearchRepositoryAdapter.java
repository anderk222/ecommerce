package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import java.util.List;

import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.port.LeadSearchCriteria;
import com.anderk.ecommerce.lead.domain.port.LeadSearchRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the search of the leads.
 */
@Component
@Transactional
public class JpaLeadSearchRepositoryAdapter implements LeadSearchRepository {

    private final LeadSearchJpaRepository leads;

    public JpaLeadSearchRepositoryAdapter(LeadSearchJpaRepository leads) {
        this.leads = leads;
    }

    @Override
    @Transactional(readOnly = true)
    public DomainPage<Lead> search(LeadSearchCriteria criteria, PageRequestDto page) {
        List<Lead> all = this.findAll(criteria);
        int from = Math.min(page.offset(), all.size());
        int to = Math.min(from + page.size(), all.size());
        List<Lead> content = all.subList(from, to);
        return DomainPage.of(content, page.page(), page.size(), all.size());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lead> findAll(LeadSearchCriteria criteria) {
        // The pattern arrives already lowercased: the columns are compared
        // with lower(), and a null parameter inside lower() has no type in
        // PostgreSQL.
        String queryPattern = criteria.hasQuery()
                ? "%" + criteria.query().trim().toLowerCase(java.util.Locale.ROOT) + "%"
                : null;
        String sourcePattern = criteria.source() == null ? null : "%" + criteria.source().name() + "%";

        List<LeadJpaEntity> found = criteria.createdFrom() == null && criteria.createdTo() == null
                ? this.leads.search(queryPattern, criteria.status(), criteria.province(), sourcePattern)
                : this.leads.searchInRange(queryPattern, criteria.status(), criteria.province(), sourcePattern,
                        criteria.createdFrom() == null ? java.time.Instant.EPOCH : criteria.createdFrom(),
                        criteria.createdTo() == null ? java.time.Instant.now() : criteria.createdTo());

        return found.stream()
                .map(JpaLeadSearchRepositoryAdapter::toDomain)
                .toList();
    }

    private static Lead toDomain(LeadJpaEntity entity) {
        return new Lead(
                entity.getId(),
                entity.getName(),
                entity.getEmail(),
                entity.getPhone(),
                entity.getProvince(),
                entity.getStatus(),
                entity.sourceSet(),
                entity.getSaleAmount(),
                entity.getOutcome(),
                entity.isConsent(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }
}
