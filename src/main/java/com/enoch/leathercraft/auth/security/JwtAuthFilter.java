package com.enoch.leathercraft.auth.security;

import com.enoch.leathercraft.auth.repo.UserRepository;
import com.enoch.leathercraft.auth.service.JwtService;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import java.io.IOException;

@Component
public class JwtAuthFilter extends org.springframework.web.filter.OncePerRequestFilter {

    private final JwtService jwt;
    private final UserRepository repo;

    public JwtAuthFilter(JwtService jwt, UserRepository repo) {
        this.jwt = jwt; this.repo = repo;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        String header = req.getHeader("Authorization");
        if (StringUtils.hasText(header) && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwt.isValid(token)) {
                String username = jwt.extractUsername(token);
                var optUser = repo.findByUsername(username);
                if (optUser.isPresent() && SecurityContextHolder.getContext().getAuthentication() == null) {
                    var u = optUser.get();
                    var springUser = User.withUsername(u.getUsername())
                            .password(u.getPassword())
                            .roles(u.getRole().name())
                            .build();
                    var auth = new UsernamePasswordAuthenticationToken(springUser, null, springUser.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(req));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                }
            }
        }
        chain.doFilter(req, res);
    }
}
