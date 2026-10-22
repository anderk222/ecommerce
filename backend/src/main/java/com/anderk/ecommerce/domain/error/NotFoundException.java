package com.anderk.ecommerce.domain.error;

/** The requested item, lead or slug does not exist, or is not available. */
public class NotFoundException extends BusinessException {

    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }

    public NotFoundException(String message, ErrorDetail... details) {
        super(ErrorCode.NOT_FOUND, message, java.util.List.of(details));
    }
}
