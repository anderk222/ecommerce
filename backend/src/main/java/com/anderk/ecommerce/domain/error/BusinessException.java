package com.anderk.ecommerce.domain.error;

import java.util.List;


/**
 * The root of the business exception hierarchy.
 *
 * <p>A business exception carries an {@link ErrorCode} — which decides the
 * HTTP status — and a list of field-level details. It is translated to the
 * single error envelope by the handler of the adapter, and nowhere else.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode code;

    private final transient List<ErrorDetail> details;

    public BusinessException(ErrorCode code, String message) {
        this(code, message, List.of());
    }

    public BusinessException(ErrorCode code, String message, List<ErrorDetail> details) {
        super(message);
        this.code = code;
        this.details = details == null ? List.of() : List.copyOf(details);
    }

    public ErrorCode code() {
        return this.code;
    }

    public List<ErrorDetail> details() {
        return this.details;
    }
}
