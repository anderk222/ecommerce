package com.anderk.ecommerce.auth.adapter.in.web;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.anderk.ecommerce.auth.adapter.in.web.dto.LoginRequest;
import com.anderk.ecommerce.auth.application.usecases.LoginUseCase;
import com.anderk.ecommerce.auth.application.usecases.LoginUseCase.LoginResult;

/**
 * The login of the staff (HU-13).
 *
 * <p>The route is open: it is the one that hands out the token.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final LoginUseCase authService;

    public AuthController(LoginUseCase authService) {
        this.authService = authService;
    }

    /** Checks the credentials and answers with a token. */
    @PostMapping("/login")
    public ResponseEntity<LoginResult> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(this.authService.login(request.toLoginUseCaseCommand()));
    }
}
