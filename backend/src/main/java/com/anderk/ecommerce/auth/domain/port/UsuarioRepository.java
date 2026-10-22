package com.anderk.ecommerce.auth.domain.port;

import java.util.Optional;

import com.anderk.ecommerce.auth.domain.model.Usuario;

/**
 * The outbound port of the staff users.
 */
public interface UsuarioRepository {

    /** The user with that identifier. */
    Optional<Usuario> findById(Long id);

    /** The user with that login name. */
    Optional<Usuario> findByEmail(String email);

    /** Inserts or updates a user. */
    Usuario save(Usuario usuario);
}
