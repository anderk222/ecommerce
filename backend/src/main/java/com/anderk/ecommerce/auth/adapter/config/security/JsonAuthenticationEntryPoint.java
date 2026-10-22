package com.anderk.ecommerce.auth.adapter.config.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import com.anderk.ecommerce.domain.error.ErrorCode;
import com.anderk.ecommerce.domain.error.ErrorResponse;

import tools.jackson.databind.json.JsonMapper;

/**
 * Answers 401 — with the error envelope of the API — when a route needs a
 * staff member and the request does not carry a valid token.
 */
public class JsonAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final JsonMapper jsonMapper;

    public JsonAuthenticationEntryPoint(JsonMapper jsonMapper) {
        this.jsonMapper = jsonMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException exception) throws IOException {

        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(this.jsonMapper.writeValueAsString(
                ErrorResponse.of(ErrorCode.UNAUTHORIZED, "La petición necesita un token válido", List.of())));
    }
}
