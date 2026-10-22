package com.anderk.ecommerce.adapter.in.web.config;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.anderk.ecommerce.domain.error.BusinessException;
import com.anderk.ecommerce.domain.error.ErrorCode;
import com.anderk.ecommerce.domain.error.ErrorDetail;
import com.anderk.ecommerce.domain.error.ErrorResponse;

/**
 * The single translator from exceptions to the API error envelope.
 *
 * <p>Every error the API returns — business or technical — is produced here,
 * so the shape is the same everywhere: {@code { code, message, details,
 * timestamp }}. A controller never builds an error response by hand.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException exception) {
        return ResponseEntity.status(ErrorStatusMapper.statusOf(exception.code()))
                .body(ErrorResponse.of(exception.code(), exception.getMessage(), exception.details()));
    }

    /** Bean Validation on a {@code @RequestBody}. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleInvalidBody(MethodArgumentNotValidException exception) {
        List<ErrorDetail> details = new ArrayList<>();
        for (FieldError error : exception.getBindingResult().getFieldErrors()) {
            details.add(ErrorDetail.of(error.getField(), error.getDefaultMessage()));
        }
        for (var error : exception.getBindingResult().getGlobalErrors()) {
            details.add(ErrorDetail.of(error.getObjectName(), error.getDefaultMessage()));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Los datos enviados no son válidos", details));
    }

    /** A required query parameter is missing. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Falta un parámetro obligatorio",
                        List.of(ErrorDetail.of(exception.getParameterName(), "es obligatorio"))));
    }

    /** A query parameter has the wrong type. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "Un parámetro tiene un valor inválido",
                        List.of(ErrorDetail.of(exception.getName(), "valor inválido"))));
    }

    /** The request body cannot be parsed. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(ErrorCode.VALIDATION_ERROR, "El cuerpo de la petición no se puede leer", List.of()));
    }

    /** Spring Security rejects the credentials. */
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException exception) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(ErrorCode.UNAUTHORIZED, "No se pudo autenticar la petición", List.of()));
    }

    /** Spring Security rejects the role. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(ErrorCode.FORBIDDEN, "No tiene permisos para esta operación", List.of()));
    }

    /** Anything the server did not anticipate. */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception exception) {
        log.error("Unhandled exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(ErrorCode.INTERNAL_ERROR, "Ocurrió un error inesperado", List.of()));
    }
}
