package com.anderk.ecommerce.adapter.in.web.config;

import java.util.Map;

import org.springframework.http.HttpStatus;

import com.anderk.ecommerce.domain.error.ErrorCode;

/**
 * The code→HTTP mapping of the API.
 *
 * <p>It lives in its own class so that {@link ErrorCode} — which the domain
 * throws — stays free of framework imports.
 */
public final class ErrorStatusMapper {

    private static final Map<ErrorCode, HttpStatus> STATUSES = Map.of(
            ErrorCode.VALIDATION_ERROR, HttpStatus.BAD_REQUEST,
            ErrorCode.UNAUTHORIZED, HttpStatus.UNAUTHORIZED,
            ErrorCode.FORBIDDEN, HttpStatus.FORBIDDEN,
            ErrorCode.NOT_FOUND, HttpStatus.NOT_FOUND,
            ErrorCode.INSUFFICIENT_STOCK, HttpStatus.CONFLICT,
            ErrorCode.INVALID_STATUS_TRANSITION, HttpStatus.CONFLICT,
            ErrorCode.CONFLICT, HttpStatus.CONFLICT,
            ErrorCode.INTERNAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR);

    private ErrorStatusMapper() {
    }

    public static HttpStatus statusOf(ErrorCode code) {
        return STATUSES.getOrDefault(code, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
