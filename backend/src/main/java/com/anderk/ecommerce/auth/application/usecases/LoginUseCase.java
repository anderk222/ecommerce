package com.anderk.ecommerce.auth.application.usecases;

/**
 * The port of the login (HU-13).
 */
public interface LoginUseCase {

    /**
     * Checks the credentials and issues a token.
     *
     * @throws com.anderk.ecommerce.domain.error.UnauthenticatedException if the email or the password
     *         are wrong
     */
    LoginResponse login(LoginRequest request);

    /**
     * The body of the login.
     *
     * @param email the login name
     * @param password the password, in clear
     */
    record LoginRequest(String email, String password) {
    }

    /**
     * The answer of the login.
     *
     * @param token the JWT
     * @param email the login name
     * @param nombre the display name
     * @param role {@code ADMIN}
     * @param expiresIn the lifetime of the token, in seconds
     */
    record LoginResponse(String token, String email, String nombre, String role, long expiresIn) {
    }
}
