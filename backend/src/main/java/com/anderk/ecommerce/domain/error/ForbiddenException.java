package com.anderk.ecommerce.domain.error;

/** Authenticated, but the role does not grant access to the resource. */
public class ForbiddenException extends BusinessException {

    public ForbiddenException(String message) {
        super(ErrorCode.FORBIDDEN, message);
    }
}
