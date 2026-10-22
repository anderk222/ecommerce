package com.anderk.ecommerce.domain.error;

import java.util.List;

/** The data is invalid, or a required consent is missing. */
public class ValidationException extends BusinessException {

    public ValidationException(String message) {
        super(ErrorCode.VALIDATION_ERROR, message);
    }

    public ValidationException(String message, List<ErrorDetail> details) {
        super(ErrorCode.VALIDATION_ERROR, message, details);
    }

    public ValidationException(String message, ErrorDetail... details) {
        super(ErrorCode.VALIDATION_ERROR, message, List.of(details));
    }
}
