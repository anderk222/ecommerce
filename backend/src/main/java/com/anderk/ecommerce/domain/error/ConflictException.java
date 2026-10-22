package com.anderk.ecommerce.domain.error;

import java.util.List;

/** The request conflicts with the current state of a resource. */
public class ConflictException extends BusinessException {

    public ConflictException(String message) {
        super(ErrorCode.CONFLICT, message);
    }

    public ConflictException(String message, List<ErrorDetail> details) {
        super(ErrorCode.CONFLICT, message, details);
    }
}
