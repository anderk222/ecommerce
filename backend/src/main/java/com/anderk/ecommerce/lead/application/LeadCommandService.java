package com.anderk.ecommerce.lead.application;

import java.math.BigDecimal;

import com.anderk.ecommerce.lead.application.view.LeadView;
import com.anderk.ecommerce.lead.domain.model.LeadOutcome;
import com.anderk.ecommerce.lead.domain.model.LeadStatus;
import com.anderk.ecommerce.lead.domain.model.Province;

/**
 * The inbound port of the **writes** of the leads (HU-21, HU-22).
 */
public interface LeadCommandService {

    /**
     * Creates a lead by hand, with the origin `MANUAL`.
     */
    LeadView create(CreateLeadCommand command);

    /**
     * Edits the data of a lead.
     */
    LeadView update(Long id, UpdateLeadCommand command);

    /**
     * Changes the state of a lead.
     *
     * <p>The transition is validated by the {@code LeadStatusPolicy}: the
     * backend refuses the ones that are not allowed.
     *
     * @throws com.anderk.ecommerce.domain.error.InvalidStatusTransitionException if the transition
     *         is not allowed
     */
    LeadView changeStatus(Long id, ChangeStatusCommand command);

    /** Deletes a lead. */
    void delete(Long id);

    /** Writes a note about a lead. */
    LeadNoteView addNote(Long id, String text);

    /**
     * The creation of a lead by hand.
     *
     * @param name the name
     * @param email the email
     * @param phone the telephone
     * @param province the province of Ecuador
     * @param saleAmount the amount of the sale
     * @param consent whether the lead agreed to be contacted
     */
    record CreateLeadCommand(
            String name,
            String email,
            String phone,
            Province province,
            BigDecimal saleAmount,
            boolean consent) {
    }

    /**
     * The edition of a lead.
     *
     * @param name the name
     * @param phone the telephone
     * @param province the province of Ecuador
     * @param saleAmount the amount of the sale
     * @param consent whether the lead agreed to be contacted
     */
    record UpdateLeadCommand(
            String name,
            String phone,
            Province province,
            BigDecimal saleAmount,
            boolean consent) {
    }

    /**
     * The change of the state of a lead.
     *
     * @param status the new state
     * @param outcome the result, when the lead is closed
     * @param saleAmount the amount of the sale, when the lead is closed
     */
    record ChangeStatusCommand(LeadStatus status, LeadOutcome outcome, BigDecimal saleAmount) {
    }
}
