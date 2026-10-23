package com.anderk.ecommerce.auth.adapter.in.web.validation;

import com.anderk.ecommerce.domain.model.vo.Email;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class EmailValueValidator implements ConstraintValidator<ValidEmail, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Delegar a @NotBlank
        }
        try {
            new Email(value); // VO, con la regla real
            return true;
        } catch (IllegalArgumentException exception) { 
            return false;
        }
    }
}