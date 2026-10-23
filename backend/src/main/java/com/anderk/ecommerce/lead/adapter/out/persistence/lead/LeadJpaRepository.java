package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * The Spring Data repository of the leads.
 *
 * <p>The search is not declared here: {@link JpaSpecificationExecutor} runs the
 * {@code Specification} that {@link LeadSpecifications} builds from the
 * criteria, and the {@code Sort} that {@link LeadSorting} builds from the
 * ordering. A new filter or a new ordering is one more entry in those classes,
 * not another method with a growing list of parameters.
 */
public interface LeadJpaRepository
        extends JpaRepository<LeadJpaEntity, Long>, JpaSpecificationExecutor<LeadJpaEntity> {

    Optional<LeadJpaEntity> findByEmail(String email);
}
