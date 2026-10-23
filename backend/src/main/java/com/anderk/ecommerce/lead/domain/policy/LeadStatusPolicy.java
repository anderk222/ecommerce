package com.anderk.ecommerce.lead.domain.policy;

import com.anderk.ecommerce.domain.error.InvalidStatusTransitionException;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

/**
 * The rules of the pipeline of the leads (HU-22).
 *
 * <p>The flow is {@code Nuevo → En gestión → Cerrado}, and a closed lead can
 * be reopened. There is **no** {@code Nuevo → Cerrado}: a lead that was never
 * worked on cannot be closed.
 *
 * <p>The rule lives in the backend. The frontend may be smarter, but it may
 * not be *different* (mission §7.8).
 */
public class LeadStatusPolicy {

    /** Whether the transition is allowed. */
    public boolean canTransition(LeadStatus from, LeadStatus to) {
        if (from == null || to == null || from == to) {
            return false;
        }
        return switch (from) {
            case NEW -> to == LeadStatus.IN_PROGRESS;
            case IN_PROGRESS -> to == LeadStatus.CLOSED;
            case CLOSED -> to == LeadStatus.IN_PROGRESS;
        };
    }

    /**
     * Asserts that the transition is allowed.
     *
     * @throws InvalidStatusTransitionException if it is not
     */
    public void assertAllowed(LeadStatus from, LeadStatus to) {
        if (!canTransition(from, to)) {
            throw new InvalidStatusTransitionException(
                    "No se puede pasar de «" + label(from) + "» a «" + label(to) + "»");
        }
    }

    private String label(LeadStatus status) {
        return switch (status) {
            case NEW -> "Nuevo";
            case IN_PROGRESS -> "En gestión";
            case CLOSED -> "Cerrado";
        };
    }
}
