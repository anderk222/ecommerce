package com.anderk.ecommerce.catalog.domain.model;

import java.time.Instant;

import com.anderk.ecommerce.domain.error.ValidationException;

/**
 * A percentage discount on a catalog item.
 *
 * <p>Mission section 7.7: one current discount per item. The discount is
 * *not* expired by a scheduled job — it is evaluated against the current time
 * every time the price is read (HU-19).
 *
 * @param id the identifier, null before it is persisted
 * @param itemId the item the discount belongs to
 * @param percentage the discount, from 0 to 100
 * @param startsAt when the discount starts, null means "immediately"
 * @param endsAt when the discount ends, null means "indefinite"
 * @param revokedAt when the discount was revoked, null means "in force"
 */
public record Discount(Long id, Long itemId, int percentage, Instant startsAt, Instant endsAt, Instant revokedAt) {

    public Discount {
        if (percentage < 0 || percentage > 100) {
            throw new ValidationException("El porcentaje del descuento debe estar entre 0 y 100",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("percentage", "debe estar entre 0 y 100"));
        }
        if (startsAt != null && endsAt != null && endsAt.isBefore(startsAt)) {
            throw new ValidationException("La fecha de fin del descuento no puede ser anterior a la de inicio",
                    com.anderk.ecommerce.domain.error.ErrorDetail.of("endsAt", "anterior a startsAt"));
        }
    }

    /**
     * Whether the discount applies at a given instant.
     *
     * <p>A discount is in force when it was not revoked and the instant falls
     * inside its window. A null start means "since the beginning", a null end
     * means "indefinite".
     */
    public boolean isEffectiveAt(Instant now) {
        if (revokedAt != null) {
            return false;
        }
        if (startsAt != null && now.isBefore(startsAt)) {
            return false;
        }
        return endsAt == null || !now.isAfter(endsAt);
    }

    /** Whether the discount was revoked manually. */
    public boolean isRevoked() {
        return revokedAt != null;
    }
}
