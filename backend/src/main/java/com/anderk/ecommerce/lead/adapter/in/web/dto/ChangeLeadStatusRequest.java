package com.anderk.ecommerce.lead.adapter.in.web.dto;

import java.math.BigDecimal;

import com.anderk.ecommerce.lead.domain.model.enums.LeadOutcome;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

/**
 * The body of the change of the state of a lead.
 *
 * @param status     the new state
 * @param outcome    the result, when the lead is closed
 * @param saleAmount the amount of the sale, when the lead is closed
 */
public record ChangeLeadStatusRequest(
        LeadStatus status,
        LeadOutcome outcome,
        BigDecimal saleAmount) {
}