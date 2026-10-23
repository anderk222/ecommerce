package com.anderk.ecommerce.domain.model.vo;

import java.util.Locale;
import java.util.regex.Pattern;

import com.anderk.ecommerce.domain.error.ValidationException;

public record Email(String value) {

    private static final int MAX_LENGTH = 254;
    private static final Pattern PATTERN = Pattern.compile(
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$"
    );

    public Email {
        if (value == null || value.isBlank()) {
            throw new ValidationException("El email no puede estar vacío");
        }

        value = value.trim().toLowerCase(Locale.ROOT);

        if (value.length() > MAX_LENGTH) {
            throw new ValidationException("El email excede " + MAX_LENGTH + " caracteres");
        }
        if (!PATTERN.matcher(value).matches()) {
            throw new ValidationException("Formato de email inválido: " + value);
        }
    }

    @Override
    public String toString() {
        return value;
    }
}