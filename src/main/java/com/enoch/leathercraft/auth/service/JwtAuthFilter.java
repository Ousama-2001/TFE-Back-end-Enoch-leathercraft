package com.enoch.leathercraft.auth.service;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Slf4j
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

        String path = request.getRequestURI();
        String authHeader = request.getHeader("Authorization");

        log.info("[JWT-FILTER] Requête sur {} - Authorization header = {}", path, authHeader);

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.info("[JWT-FILTER] Pas de bearer token, on continue sans auth");
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        log.info("[JWT-FILTER] Token reçu (tronqué) = {}", token.length() > 30 ? token.substring(0, 30) + "..." : token);

        try {
            String email = jwtService.extractSubject(token);   // sub = email
            String role  = jwtService.extractRole(token);      // "CUSTOMER" ou "ADMIN"
            log.info("[JWT-FILTER] Token décodé -> email={}, role={}", email, role);

            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                var authorities = List.of(new SimpleGrantedAuthority(role));

                var authToken = new UsernamePasswordAuthenticationToken(
                        email,
                        null,
                        authorities
                );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);

                log.info("[JWT-FILTER] Authentication placée dans le SecurityContext : {}", authToken);
            }

        } catch (Exception e) {
            log.error("[JWT-FILTER] Erreur en décodant le token : {}", e.getMessage(), e);
        }

        filterChain.doFilter(request, response);
    }
}
