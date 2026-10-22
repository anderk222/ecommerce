package com.anderk.ecommerce.lead.adapter.out.persistence.lead;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.anderk.ecommerce.lead.domain.model.LeadOutcome;
import com.anderk.ecommerce.lead.domain.model.LeadSource;
import com.anderk.ecommerce.lead.domain.model.LeadStatus;
import com.anderk.ecommerce.lead.domain.model.Province;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * The persistence of a lead.
 */
@Entity
@Table(name = "leads")
public class LeadJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "email", nullable = false, unique = true)
    private String email;

    @Column(name = "phone", length = 64)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "province", length = 64)
    private Province province;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private LeadStatus status;

    /** The origins, separated by commas. */
    @Column(name = "sources", nullable = false)
    private String sources;

    @Column(name = "sale_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal saleAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome", length = 32)
    private LeadOutcome outcome;

    @Column(name = "consent", nullable = false)
    private boolean consent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return this.email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public Province getProvince() {
        return this.province;
    }

    public void setProvince(Province province) {
        this.province = province;
    }

    public LeadStatus getStatus() {
        return this.status;
    }

    public void setStatus(LeadStatus status) {
        this.status = status;
    }

    public String getSources() {
        return this.sources;
    }

    public void setSources(String sources) {
        this.sources = sources;
    }

    public BigDecimal getSaleAmount() {
        return this.saleAmount;
    }

    public void setSaleAmount(BigDecimal saleAmount) {
        this.saleAmount = saleAmount;
    }

    public LeadOutcome getOutcome() {
        return this.outcome;
    }

    public void setOutcome(LeadOutcome outcome) {
        this.outcome = outcome;
    }

    public boolean isConsent() {
        return this.consent;
    }

    public void setConsent(boolean consent) {
        this.consent = consent;
    }

    public Instant getCreatedAt() {
        return this.createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return this.updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    /** The origins of the lead, as a set. */
    public Set<LeadSource> sourceSet() {
        if (this.sources == null || this.sources.isBlank()) {
            return EnumSet.noneOf(LeadSource.class);
        }
        return Arrays.stream(this.sources.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(LeadSource::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(LeadSource.class)));
    }
}
