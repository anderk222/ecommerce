package com.anderk.ecommerce.catalog.domain.vo;

import java.math.BigDecimal;
import java.math.RoundingMode;

import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.ValidationException;

/**
 * An amount of money.
 *
 * <p>The currency is part of the value: the system sells in dollars
 * (mission section 6), but the price of a service in a currency that is not
 * the one of a product is a different amount, not the same number.
 *
 * @param amount the amount, never negative, always with two decimals
 * @param currency the ISO currency code
 */
public record Money(BigDecimal amount, String currency) {

    public static final String DEFAULT_CURRENCY = "USD";

    private static final int SCALE = 2;

    public Money {
        if (amount == null) {
            throw new ValidationException("El monto no puede ser nulo");
        }
        if (amount.signum() < 0) {
            throw new ValidationException("El monto no puede ser negativo: " + amount,
                    ErrorDetail.of("price", "no puede ser negativo"));
        }
        if (currency == null || currency.isBlank()) {
            throw new ValidationException("La moneda es obligatoria");
        }
        amount = amount.setScale(SCALE, RoundingMode.HALF_UP);
        currency = currency.toUpperCase(java.util.Locale.ROOT);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money zero() {
        return new Money(BigDecimal.ZERO, DEFAULT_CURRENCY);
    }

    public Money plus(Money other) {
        this.requireSameCurrency(other);
        return new Money(this.amount.add(other.amount), this.currency);
    }

    public Money minus(Money other) {
        this.requireSameCurrency(other);
        BigDecimal result = this.amount.subtract(other.amount);
        if (result.signum() < 0) {
            throw new ValidationException("El resultado no puede ser negativo");
        }
        return new Money(result, this.currency);
    }

    public Money times(BigDecimal factor) {
        return new Money(this.amount.multiply(factor), this.currency);
    }

    /** The amount left after a percentage discount. */
    public Money discountedBy(int percentage) {
        if (percentage < 0 || percentage > 100) {
            throw new ValidationException("El porcentaje debe estar entre 0 y 100");
        }
        BigDecimal factor = BigDecimal.valueOf(100 - percentage)
                .divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP);
        return new Money(this.amount.multiply(factor), this.currency);
    }

    public boolean isZero() {
        return this.amount.signum() == 0;
    }

    public int compareTo(Money other) {
        this.requireSameCurrency(other);
        return this.amount.compareTo(other.amount);
    }

    private void requireSameCurrency(Money other) {
        if (!this.currency.equals(other.currency)) {
            throw new ValidationException("No se pueden combinar montos en monedas distintas: "
                    + this.currency + " y " + other.currency);
        }
    }

    @Override
    public String toString() {
        return this.currency + " " + this.amount;
    }
}
