package com.anderk.ecommerce.auth.domain.model;

import com.anderk.ecommerce.domain.model.vo.Email;

/**
 * A member of the staff: the administrator.
 *
 * <p>It is the domain model of the {@code auth} feature. The persistence
 * mapping arrives with the feature itself, in P10.
 *
 * @param id the identifier
 * @param nombre the display name
 * @param email the login name
 * @param password the BCrypt hash of the password — never the password itself
 * @param role the role: {@code ADMIN}
 */
public record Usuario(Long id, String nombre, Email email, String password, String role) {

    public static final String ROLE_ADMIN = "ADMIN";

    public boolean isAdmin() {
        return ROLE_ADMIN.equals(this.role);
    }

}
