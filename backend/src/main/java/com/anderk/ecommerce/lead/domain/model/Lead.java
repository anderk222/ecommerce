package com.anderk.ecommerce.lead.domain.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;

import com.anderk.ecommerce.domain.error.ValidationException;

/**
 * A lead: a person who showed interest and left a contact (HU-21).
 *
 * <p>The identity of a lead is its email. The same person who writes from two
 * devices is **one** lead with two origins and two browsers (HU-10, HU-11).
 *
 * @param id the identifier
 * @param name the name
 * @param email the email, the identity of the lead
 * @param phone the telephone
 * @param province the province of Ecuador
 * @param status the state of the pipeline
 * @param sources where the lead came from
 * @param saleAmount the amount of the sale, in dollars
 * @param outcome how the lead ended, when it is closed
 * @param consent whether the lead agreed to be contacted
 * @param createdAt when the lead arrived
 * @param updatedAt when the lead was touched for the last time
 */
public record Lead(
        Long id,
        String name,
        String email,
        String phone,
        Province province,
        LeadStatus status,
        Set<LeadSource> sources,
        BigDecimal saleAmount,
        LeadOutcome outcome,
        boolean consent,
        Instant createdAt,
        Instant updatedAt) {

    public Lead {
        if (name == null || name.isBlank()) {
            throw new ValidationException("El nombre del lead es obligatorio");
        }
        if (email == null || email.isBlank()) {
            throw new ValidationException("El correo del lead es obligatorio");
        }
        status = status == null ? LeadStatus.NEW : status;
        sources = sources == null || sources.isEmpty()
                ? EnumSet.noneOf(LeadSource.class)
                : EnumSet.copyOf(sources);
        saleAmount = saleAmount == null ? BigDecimal.ZERO : saleAmount;
    }

    /** A copy with another origin. The origins are never lost. */
    public Lead withSource(LeadSource source) {
        Set<LeadSource> merged = EnumSet.noneOf(LeadSource.class);
        merged.addAll(this.sources);
        merged.add(source);
        return new Lead(this.id, this.name, this.email, this.phone, this.province, this.status,
                merged, this.saleAmount, this.outcome, this.consent, this.createdAt, this.updatedAt);
    }

    /** A copy with the state changed. */
    public Lead withStatus(LeadStatus newStatus, LeadOutcome newOutcome, BigDecimal newSaleAmount) {
        return new Lead(this.id, this.name, this.email, this.phone, this.province, newStatus,
                this.sources, newSaleAmount == null ? this.saleAmount : newSaleAmount,
                newOutcome, this.consent, this.createdAt, Instant.now());
    }

    /** A copy with the amount of the sale. The amount never goes down. */
    public Lead withSaleAmount(java.math.BigDecimal amount) {
        if (amount == null || amount.signum() <= 0) {
            return this;
        }
        return new Lead(this.id, this.name, this.email, this.phone, this.province, this.status,
                this.sources, amount, this.outcome, this.consent, this.createdAt, Instant.now());
    }

    /** A copy with the details of the contact updated. */
    public Lead withContact(String newName, String newPhone, Province newProvince, boolean newConsent) {
        return new Lead(this.id,
                newName == null || newName.isBlank() ? this.name : newName,
                this.email,
                newPhone == null || newPhone.isBlank() ? this.phone : newPhone,
                newProvince == null ? this.province : newProvince,
                this.status, this.sources, this.saleAmount, this.outcome,
                this.consent || newConsent, this.createdAt, Instant.now());
    }
}
