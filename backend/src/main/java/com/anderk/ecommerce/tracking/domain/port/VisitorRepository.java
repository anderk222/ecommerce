package com.anderk.ecommerce.tracking.domain.port;

import java.util.Optional;

import com.anderk.ecommerce.tracking.domain.model.Visitor;

/**
 * The outbound port of the registry of the browsers.
 */
public interface VisitorRepository {

    /** Inserts or updates a visitor. */
    Visitor save(Visitor visitor);

    /** The visitor with that anonymous identifier. */
    Optional<Visitor> findByAnonymousId(String anonymousId);

    /** Links a browser with a lead. */
    void linkToLead(String anonymousId, Long leadId);
}
