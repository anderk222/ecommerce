package com.anderk.ecommerce.auth.domain.model;

import java.security.Principal;

/**
 * The member of the staff that is signed in.
 *
 * <p>It is the principal of the security context: the token carries it.
 *
 * @param id the identifier of the user
 * @param email the login name
 * @param nombre the display name
 * @param role {@code ADMIN}
 */
public record AuthenticatedUser(Long id, String email, String nombre, String role)
        implements Principal {

    /** The name of the principal is its email: it is what the security context shows. */
    @Override
    public String getName() {
        return this.email;
    }


    public boolean isAdmin() {
        return Usuario.ROLE_ADMIN.equals(this.role);
    }

}
