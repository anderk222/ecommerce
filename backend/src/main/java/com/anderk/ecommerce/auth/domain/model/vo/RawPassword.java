package com.anderk.ecommerce.auth.domain.model.vo;

public record RawPassword(String value) {

    private static final int MIN_LENGTH = 8;
    private static final int MAX_LENGTH = 64;

    public RawPassword {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        if (value.length() < MIN_LENGTH) {
            throw new IllegalArgumentException("La contraseña debe tener al menos " + MIN_LENGTH + " caracteres");
        }
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("La contraseña no puede exceder " + MAX_LENGTH + " caracteres");
        }
    }

    @Override
    public String toString() {
        return "RawPassword[PROTECTED]";
    }
}