package com.anderk.ecommerce.auth.adapter.in.web.validation;

import com.anderk.ecommerce.auth.domain.model.vo.RawPassword;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValueValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Delegar a @NotBlank
        }
        try {
            new RawPassword(value); // VO, con la regla real
            return true;
        } catch (IllegalArgumentException exception) { 
            return false;
        }
    }
    
}
