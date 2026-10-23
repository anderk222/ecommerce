package com.anderk.ecommerce.lead.application.view;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.anderk.ecommerce.lead.domain.model.Province;
import com.anderk.ecommerce.lead.domain.model.enums.LeadOutcome;
import com.anderk.ecommerce.lead.domain.model.enums.LeadSource;
import com.anderk.ecommerce.lead.domain.model.enums.LeadStatus;

/**
 * A lead as the listing and the detail answer it (HU-21, HU-25).
 *
 * @param id the identifier
 * @param name the name
 * @param email the email, the identity of the lead
 * @param phone the telephone
 * @param province the province of Ecuador
 * @param status the state of the pipeline
 * @param sources where the lead came from
 * @param saleAmount the amount of the sale
 * @param outcome how the lead ended, when it is closed
 * @param consent whether the lead agreed to be contacted
 * @param createdAt when the lead arrived
 * @param updatedAt when the lead was touched for the last time
 */
public record LeadView(
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

    public LeadView {
        sources = sources == null ? List.of() : List.copyOf(sources);
    }
}
