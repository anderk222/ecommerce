package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.anderk.ecommerce.domain.pagination.DomainPage;
import com.anderk.ecommerce.domain.pagination.PageRequestDto;
import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;
import com.anderk.ecommerce.lead.domain.search.LeadSearchCriteria;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the leads: JPA on one side, the outbound port on the
 * other.
 */
@Transactional
public class JpaLeadRepositoryAdapter implements LeadRepository {

    private final LeadJpaRepository leads;

    public JpaLeadRepositoryAdapter(LeadJpaRepository leads) {
        this.leads = leads;
    }

    @Override
    public Lead save(Lead lead) {
        LeadJpaEntity entity = lead.id() == null
                ? new LeadJpaEntity()
                : this.leads.findById(lead.id()).orElseGet(LeadJpaEntity::new);
        entity.setId(lead.id());
        entity.setName(lead.name());
        entity.setEmail(lead.email());
        entity.setPhone(lead.phone());
        entity.setProvince(lead.province());
        entity.setStatus(lead.status());
        entity.setSources(lead.sources().stream().map(Enum::name).collect(Collectors.joining(",")));
        entity.setSaleAmount(lead.saleAmount());
        entity.setOutcome(lead.outcome());
        entity.setConsent(lead.consent());
        if (lead.createdAt() != null) {
            entity.setCreatedAt(lead.createdAt());
        }
        entity.setUpdatedAt(java.time.Instant.now());
        return toDomain(this.leads.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lead> findById(Long id) {
        return this.leads.findById(id).map(JpaLeadRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Lead> findByEmail(String email) {
        return this.leads.findByEmail(email).map(JpaLeadRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public DomainPage<Lead> search(LeadSearchCriteria criteria, PageRequestDto page) {
        Pageable pageable = PageRequest.of(page.page(), page.size(), LeadSorting.of(criteria.sort()));
        Page<LeadJpaEntity> found = this.leads.findAll(LeadSpecifications.from(criteria), pageable);
        List<Lead> content = found.getContent().stream()
                .map(JpaLeadRepositoryAdapter::toDomain)
                .toList();
        return DomainPage.of(content, found.getNumber(), found.getSize(), found.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Lead> findAll(LeadSearchCriteria criteria) {
        Sort sort = LeadSorting.of(criteria.sort());
        return this.leads.findAll(LeadSpecifications.from(criteria), sort).stream()
                .map(JpaLeadRepositoryAdapter::toDomain)
                .toList();
    }

    @Override
    public void deleteById(Long id) {
        this.leads.deleteById(id);
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
