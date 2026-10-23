package com.anderk.ecommerce.auth.adapter.in.web.config.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;

import com.anderk.ecommerce.domain.error.ErrorCode;
import com.anderk.ecommerce.domain.error.ErrorResponse;

import tools.jackson.databind.json.JsonMapper;

/**
 * Answers 401 when the caller is anonymous and 403 when the role is not
 * enough — with the error envelope of the API, not with an empty body.
 */
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

    private final JsonMapper jsonMapper;

    public JsonAccessDeniedHandler(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException exception) throws IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean anonymous = authentication == null || authentication instanceof AnonymousAuthenticationToken;

        ErrorCode code = anonymous ? ErrorCode.UNAUTHORIZED : ErrorCode.FORBIDDEN;
        String message = anonymous
                ? "La petición necesita un token válido"
                : "Su rol no permite esta operación";

        response.setStatus(code == ErrorCode.UNAUTHORIZED
                ? HttpStatus.UNAUTHORIZED.value()
                : HttpStatus.FORBIDDEN.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(this.jsonMapper.writeValueAsString(
                ErrorResponse.of(code, message, List.of())));
    }
}
