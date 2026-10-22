package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import java.util.Optional;
import java.util.stream.Collectors;

import com.anderk.ecommerce.lead.domain.model.Lead;
import com.anderk.ecommerce.lead.domain.port.LeadRepository;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the leads.
 */
@Component
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
