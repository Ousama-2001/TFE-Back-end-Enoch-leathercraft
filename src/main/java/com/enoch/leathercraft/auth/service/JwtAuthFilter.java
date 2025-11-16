// com/enoch/leathercraft/auth/service/JwtAuthFilter.java
package com.enoch.leathercraft.auth.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);

        try {
            String email = jwtService.extractSubject(token);   // sub = email
            String role = jwtService.extractRole(token);       // "CUSTOMER" ou "ADMIN"

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Spring s’attend à "ROLE_ADMIN" / "ROLE_CUSTOMER"
                var authorities = List.of(
                        new SimpleGrantedAuthority("ROLE_" + role)
                );

                var authToken = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        } catch (Exception e) {
            // token invalide -> on laisse sans auth, les règles de sécurité bloqueront
        }

        filterChain.doFilter(request, response);
    }
}
