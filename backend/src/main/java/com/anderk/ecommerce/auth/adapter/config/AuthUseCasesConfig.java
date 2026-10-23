package com.anderk.ecommerce.auth.adapter.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.anderk.ecommerce.auth.adapter.out.jwt.TokenServiceImpl;
import com.anderk.ecommerce.auth.adapter.out.persistence.JpaUsuarioRepositoryAdapter;
import com.anderk.ecommerce.auth.adapter.out.persistence.UsuarioJpaRepository;
import com.anderk.ecommerce.auth.application.out.TokenService;
import com.anderk.ecommerce.auth.application.usecases.LoginUseCase;
import com.anderk.ecommerce.auth.application.usecases.LoginUseCaseImpl;
import com.anderk.ecommerce.auth.domain.port.UsuarioRepository;

@Configuration
public class AuthUseCasesConfig {

    @Bean
    UsuarioRepository usuarioRepository(UsuarioJpaRepository usuarios) {
        return new JpaUsuarioRepositoryAdapter(usuarios);
    }

    @Bean
    TokenService tokenService(@Value("${app.security.jwt-secret}") String secret,
                              @Value("${app.security.jwt-ttl:3600}") long ttlSeconds) {
        return new TokenServiceImpl(secret, ttlSeconds);
    }

    @Bean
    LoginUseCase loginUseCase(@Autowired UsuarioRepository usuarioRepository,
                              @Autowired PasswordEncoder passwordEncoder,
                              @Autowired TokenService tokenService) {
        return new LoginUseCaseImpl(usuarioRepository, passwordEncoder, tokenService);
    }
}
