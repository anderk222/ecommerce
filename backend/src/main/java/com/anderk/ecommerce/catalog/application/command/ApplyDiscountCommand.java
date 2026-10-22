package com.anderk.ecommerce.catalog.application.command;

import java.time.Instant;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * The command that applies a discount to an item.
 *
 * <p>HU-19: a percentage, an optional start and an optional end. A null end
 * means the discount is indefinite. The price is calculated at read time, so
 * no scheduled process is needed to expire it.
 *
 * @param percentage the discount, from 0 to 100
 * @param startsAt when the discount starts, null means immediately
 * @param endsAt when the discount ends, null means indefinite
 */
public record ApplyDiscountCommand(
        @Min(0) @Max(100) int percentage,
        Instant startsAt,
        Instant endsAt) {
}
