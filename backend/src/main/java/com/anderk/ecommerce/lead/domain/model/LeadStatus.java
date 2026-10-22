package com.anderk.ecommerce.lead.domain.model;

/**
 * The state of a lead in the pipeline (HU-22).
 *
 * <p>The flow is {@code Nuevo → En gestión → Cerrado}, and a closed lead can
 * be reopened.
 */
public enum LeadStatus {

    /** The lead has just arrived. */
    NEW,

    /** Somebody is working on it. */
    IN_PROGRESS,

    /** The lead is closed: won or lost. */
    CLOSED
}
