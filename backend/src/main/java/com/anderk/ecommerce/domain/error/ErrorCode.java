package com.anderk.ecommerce.domain.error;


/**
 * The error codes of the whole API.
 *
 * <p>The enum is deliberately free of framework imports: the domain throws
 * business exceptions, and a domain class must not drag Spring's
 * {@code HttpStatus} in with it. The code→HTTP mapping lives in
 * the mapper of the adapter, in the other direction.
 *
 * <p>The mapping is fixed by the use-case document (section 12.5).
 */
public enum ErrorCode {

    /** Invalid data, or a missing consent. */
    VALIDATION_ERROR,

    /** No token, or an invalid one. */
    UNAUTHORIZED,

    /** Authenticated, but the role is not enough. */
    FORBIDDEN,

    /** The item, lead or slug does not exist, or is not available. */
    NOT_FOUND,

    /** The purchase asks for more stock than there is. */
    INSUFFICIENT_STOCK,

    /** The lead state machine refuses the transition. */
    INVALID_STATUS_TRANSITION,

    /** The request conflicts with the current state of a resource. */
    CONFLICT,

    /** Anything the server did not anticipate. */
    INTERNAL_ERROR
}
