package com.anderk.ecommerce.auth.adapter.in.web.config.security;

import java.io.IOException;
import java.util.List;

import com.anderk.ecommerce.auth.application.out.TokenService;
import com.anderk.ecommerce.auth.domain.model.AuthenticatedUser;
import com.anderk.ecommerce.domain.error.BusinessException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Reads the token of the header and puts the member of the staff in the
 * security context.
 *
 * <p>A request without a token, or with a token that is not valid, is **not**
 * rejected here: the chain decides. The filter only says who the caller is.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIX = "Bearer ";

    private final TokenService tokenService;

    public JwtAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith(PREFIX)) {
            try {
                AuthenticatedUser user = this.tokenService.parse(header.substring(PREFIX.length()).trim());
                var authentication = new UsernamePasswordAuthenticationToken(
                        user, null, List.of(new SimpleGrantedAuthority("ROLE_" + user.role())));
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (BusinessException exception) {
                // The token is not valid: the caller stays anonymous, and the
                // chain answers 401 if the route needs a staff member.
                SecurityContextHolder.clearContext();
            }
        }

        chain.doFilter(request, response);
    }
}
