package com.anderk.ecommerce.auth.application.usecases;

import com.anderk.ecommerce.auth.application.out.TokenService;
import com.anderk.ecommerce.auth.domain.model.AuthenticatedUser;
import com.anderk.ecommerce.auth.domain.model.Usuario;
import com.anderk.ecommerce.auth.domain.port.UsuarioRepository;
import com.anderk.ecommerce.domain.error.UnauthenticatedException;

import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The login of the staff (HU-13).
 */
public class LoginUseCaseImpl implements LoginUseCase {

    private final UsuarioRepository usuarioRepository;

    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    public LoginUseCaseImpl(UsuarioRepository usuarioRepository,
                           PasswordEncoder passwordEncoder,
                           TokenService tokenService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
    }

    @Override
    public LoginResult login(LoginCommand request) {

        Usuario usuario = this.usuarioRepository
                .findByEmail(request.email().toString().trim().toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new UnauthenticatedException("Las credenciales no son correctas"));

        if (!this.passwordEncoder.matches(request.password().value(), usuario.password())) {
            throw new UnauthenticatedException("Las credenciales no son correctas");
        }

        AuthenticatedUser authenticated = new AuthenticatedUser(
                usuario.id(), usuario.email(), usuario.nombre(), usuario.role());

        return new LoginResult(
                this.tokenService.issue(authenticated),
                usuario.email().value(),
                usuario.nombre(),
                usuario.role(),
                this.tokenService.ttlSeconds());
    }
}
