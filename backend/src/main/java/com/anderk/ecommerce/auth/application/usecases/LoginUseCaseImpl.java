package com.anderk.ecommerce.auth.application.usecases;

import com.anderk.ecommerce.auth.application.service.TokenService;
import com.anderk.ecommerce.auth.application.service.TokenServiceImpl;
import com.anderk.ecommerce.auth.domain.model.AuthenticatedUser;
import com.anderk.ecommerce.auth.domain.model.Usuario;
import com.anderk.ecommerce.auth.domain.port.UsuarioRepository;
import com.anderk.ecommerce.domain.error.UnauthenticatedException;
import com.anderk.ecommerce.shared.annotation.UseCase;

import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * The login of the staff (HU-13).
 */
@UseCase
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
    public LoginUseCase.LoginResponse login(LoginUseCase.LoginRequest request) {
        if (request == null || request.email() == null || request.password() == null) {
            throw new UnauthenticatedException("El correo y la contraseña son obligatorios");
        }

        Usuario usuario = this.usuarioRepository
                .findByEmail(request.email().trim().toLowerCase(java.util.Locale.ROOT))
                .orElseThrow(() -> new UnauthenticatedException("Las credenciales no son correctas"));

        if (!this.passwordEncoder.matches(request.password(), usuario.password())) {
            throw new UnauthenticatedException("Las credenciales no son correctas");
        }

        AuthenticatedUser authenticated = new AuthenticatedUser(
                usuario.id(), usuario.email(), usuario.nombre(), usuario.role());

        long ttl = this.tokenService instanceof TokenServiceImpl implementation
                ? implementation.ttlSeconds()
                : 3600;

        return new LoginUseCase.LoginResponse(
                this.tokenService.issue(authenticated),
                usuario.email(),
                usuario.nombre(),
                usuario.role(),
                ttl);
    }
}
