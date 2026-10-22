package com.anderk.ecommerce.auth.application.service;

import com.anderk.ecommerce.auth.domain.model.AuthenticatedUser;

/**
 * The port that issues and reads the tokens (HU-13).
 */
public interface TokenService {

    /**
     * Issues a token for a member of the staff.
     *
     * <p>The token carries the identifier, the email and the role.
     */
    String issue(AuthenticatedUser user);

    /**
     * Reads a token.
     *
     * @throws com.anderk.ecommerce.domain.error.UnauthenticatedException if the token is invalid or
     *         has expired
     */
    AuthenticatedUser parse(String token);
}
