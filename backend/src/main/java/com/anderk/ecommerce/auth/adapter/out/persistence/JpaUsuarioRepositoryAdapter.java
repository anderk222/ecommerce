package com.anderk.ecommerce.auth.adapter.out.persistence;

import java.util.Optional;

import com.anderk.ecommerce.auth.domain.model.Usuario;
import com.anderk.ecommerce.auth.domain.port.UsuarioRepository;
import com.anderk.ecommerce.domain.model.vo.Email;

import org.springframework.transaction.annotation.Transactional;

/**
 * The output adapter of the staff users.
 */
@Transactional
public class JpaUsuarioRepositoryAdapter implements UsuarioRepository {

    private final UsuarioJpaRepository usuarios;

    public JpaUsuarioRepositoryAdapter(UsuarioJpaRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> findById(Long id) {
        return this.usuarios.findById(id).map(JpaUsuarioRepositoryAdapter::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Usuario> findByEmail(String email) {
        return this.usuarios.findByEmail(email).map(JpaUsuarioRepositoryAdapter::toDomain);
    }

    @Override
    public Usuario save(Usuario usuario) {
        UsuarioJpaEntity entity = usuario.id() == null
                ? new UsuarioJpaEntity()
                : this.usuarios.findById(usuario.id()).orElseGet(UsuarioJpaEntity::new);
        entity.setId(usuario.id());
        entity.setEmail(usuario.email().toString());
        entity.setPasswordHash(usuario.password());
        entity.setDisplayName(usuario.nombre());
        entity.setRole(usuario.role());
        return toDomain(this.usuarios.save(entity));
    }

    private static Usuario toDomain(UsuarioJpaEntity entity) {
        return new Usuario(entity.getId(), entity.getDisplayName(), new Email(entity.getEmail()),
                entity.getPasswordHash(), entity.getRole());
    }
}
