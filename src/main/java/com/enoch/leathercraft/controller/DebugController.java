package com.enoch.leathercraft.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/debug")
public class DebugController {

    @GetMapping("/me")
    public Map<String, Object> me(Authentication authentication) {
        if (authentication == null) {
            return Map.of(
                    "authenticated", false,
                    "message", "Pas d'authentication dans le SecurityContext"
            );
        }

        List<String> roles = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        return Map.of(
                "authenticated", authentication.isAuthenticated(),
                "principal", authentication.getPrincipal(),
                "name", authentication.getName(),
                "authorities", roles
        );
    }
}
