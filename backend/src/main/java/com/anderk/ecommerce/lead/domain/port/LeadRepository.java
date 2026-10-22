package com.anderk.ecommerce.lead.domain.port;

import java.util.Optional;

import com.anderk.ecommerce.lead.domain.model.Lead;

/**
 * The outbound port of the leads.
 */
public interface LeadRepository {

    /** Inserts or updates a lead. */
    Lead save(Lead lead);

    /** The lead with that identifier. */
    Optional<Lead> findById(Long id);

    /** The lead with that email — the identity of a lead. */
    Optional<Lead> findByEmail(String email);

    /** Removes a lead. */
    void deleteById(Long id);
}
