package com.anderk.ecommerce.auth.adapter.config;

import com.anderk.ecommerce.auth.adapter.out.persistence.JpaUsuarioRepositoryAdapter;
import com.anderk.ecommerce.auth.adapter.out.persistence.UsuarioJpaRepository;
import com.anderk.ecommerce.auth.application.service.TokenService;
import com.anderk.ecommerce.auth.application.service.TokenServiceImpl;
import com.anderk.ecommerce.auth.domain.port.UsuarioRepository;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The explicit wiring of the {@code auth} hexagon.
 *
 * <p>Only the adapters are declared here. The use case is not: it carries the
 * {@code @UseCase} stereotype, which is a {@code @Service}, and Spring finds
 * it by component scanning. Declaring it twice — here and by the stereotype —
 * would make two beans of the same port and the context would not start.
 */
@Configuration
public class AuthConfig {

    @Bean
    UsuarioRepository usuarioRepository(UsuarioJpaRepository usuarios) {
        return new JpaUsuarioRepositoryAdapter(usuarios);
    }

    @Bean
    TokenService tokenService(@Value("${app.security.jwt-secret}") String secret,
                              @Value("${app.security.jwt-ttl:3600}") long ttlSeconds) {
        return new TokenServiceImpl(secret, ttlSeconds);
    }
}
