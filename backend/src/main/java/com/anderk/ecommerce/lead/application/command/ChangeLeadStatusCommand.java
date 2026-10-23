package com.anderk.ecommerce.lead.application.command;

import java.math.BigDecimal;

import com.anderk.ecommerce.lead.domain.model.enums.LeadOutcome;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

/**
 * The change of the state of a lead (HU-22).
 *
 * @param status the new state
 * @param outcome the result, when the lead is closed
 * @param saleAmount the amount of the sale, when the lead is closed
 */
public record ChangeLeadStatusCommand(
        LeadStatus status,
        LeadOutcome outcome,
        BigDecimal saleAmount) {
}
