package com.anderk.ecommerce.domain.error;

/** The lead state machine refuses the requested transition. */
public class InvalidStatusTransitionException extends BusinessException {

    public InvalidStatusTransitionException(String message) {
        super(ErrorCode.INVALID_STATUS_TRANSITION, message);
    }
}
