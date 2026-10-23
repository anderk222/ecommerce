package com.anderk.ecommerce.lead.application.usecases.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.lead.domain.model.enums.LeadOutcome;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;
import com.anderk.ecommerce.lead.domain.model.Province;

/**
 * A lead, as the CRM sees it.
 *
 * @param id the identifier
 * @param name the name
 * @param email the email
 * @param phone the telephone
 * @param province the province of Ecuador
 * @param status the state of the pipeline
 * @param sources where the lead came from
 * @param saleAmount the amount of the sale, in dollars
 * @param outcome how the lead ended
 * @param consent whether the lead agreed to be contacted
 * @param createdAt when the lead arrived
 * @param updatedAt when the lead was touched for the last time
 */
public record LeadResult(
        Long id,
        String name,
        String email,
        String phone,
        Province province,
        LeadStatus status,
        List<LeadSource> sources,
        BigDecimal saleAmount,
        LeadOutcome outcome,
        boolean consent,
        Instant createdAt,
        Instant updatedAt) {

    public LeadResult {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
