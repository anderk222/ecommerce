package com.anderk.ecommerce.auth.adapter.out.jwt;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

import javax.crypto.SecretKey;

import com.anderk.ecommerce.auth.application.out.TokenService;
import com.anderk.ecommerce.auth.domain.model.AuthenticatedUser;
import com.anderk.ecommerce.domain.error.UnauthenticatedException;
import com.anderk.ecommerce.domain.model.vo.Email;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import java.nio.charset.StandardCharsets;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import javax.crypto.spec.SecretKeySpec;

/**
 * The tokens of the system: HS256, issued and read with the Nimbus support
 * of Spring Security.
 *
 * <p>No third-party JWT library: the framework already has one (tech-stack
 * §2.4).
 */
public class TokenServiceImpl implements TokenService {

    private final JwtEncoder encoder;

    private final JwtDecoder decoder;

    private final long ttlSeconds;

    public TokenServiceImpl(@Value("${app.security.jwt-secret}") String secret,
                             @Value("${app.security.jwt-ttl:3600}") long ttlSeconds) {
        SecretKey key = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        this.ttlSeconds = ttlSeconds;
    }

    @Override
    public String issue(AuthenticatedUser user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("xtrim")
                .issuedAt(now)
                .expiresAt(now.plus(this.ttlSeconds, ChronoUnit.SECONDS))
                .subject(String.valueOf(user.id()))
                .claim("email", user.email().value())
                .claim("nombre", user.nombre())
                .claim("role", user.role())
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return this.encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    @Override
    public AuthenticatedUser parse(String token) {
        try {
            Jwt jwt = this.decoder.decode(token);
            return new AuthenticatedUser(
                    Long.valueOf(jwt.getSubject()),
                    new Email(jwt.getClaimAsString("email")),
                    jwt.getClaimAsString("nombre"),
                    jwt.getClaimAsString("role"));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new UnauthenticatedException("El token no es válido o ha expirado");
        }
    }

    /** The lifetime of a token, in seconds. */
    public long ttlSeconds() {
        return this.ttlSeconds;
    }

    /** The roles the system knows. */
    public List<String> roles() {
        return List.of("ADMIN");
    }
}
