package com.anderk.ecommerce.domain.error;

/** No credentials, or credentials that do not identify anybody. */
public class UnauthenticatedException extends BusinessException {

    public UnauthenticatedException(String message) {
        super(ErrorCode.UNAUTHORIZED, message);
    }
}
