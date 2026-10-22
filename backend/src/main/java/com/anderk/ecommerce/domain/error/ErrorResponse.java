package com.anderk.ecommerce.domain.error;

import java.time.Instant;
import java.util.List;

/**
 * The one error shape of the whole API.
 *
 * <pre>{@code
 * {
 *   "code": "INSUFFICIENT_STOCK",
 *   "message": "No hay stock suficiente para el ítem solicitado",
 *   "details": [{ "field": "items[0].quantity", "issue": "max 3" }],
 *   "timestamp": "2026-10-20T09:30:00Z"
 * }
 * }</pre>
 *
 * @param code the machine-readable error code
 * @param message the human-readable message
 * @param details the field-level problems, never null
 * @param timestamp when the error was produced
 */
public record ErrorResponse(String code, String message, List<ErrorDetail> details, Instant timestamp) {

    public ErrorResponse {
        details = details == null ? List.of() : List.copyOf(details);
    }

    public static ErrorResponse of(ErrorCode code, String message, List<ErrorDetail> details) {
        return new ErrorResponse(code.name(), message, details, Instant.now());
    }
}
