package com.enoch.leathercraft.config;

import com.enoch.leathercraft.auth.service.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer; // Import Important
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;

    /**
     * 🛠️ CORRECTION 403 FORBIDDEN SUR LES IMAGES
     * Ce bean permet de configurer Spring Security pour qu'il IGNORE totalement
     * les requêtes vers /uploads/**.
     * Cela contourne le filtre JWT et résout le problème d'accès aux images.
     */
    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return (web) -> web.ignoring().requestMatchers("/uploads/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults()) // Active la config CORS définie plus bas
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 🔓 Auth publique (login / register)
                        .requestMatchers("/api/auth/**").permitAll()

                        // 🔓 Lecture produits publique (Catalogue)
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // 🔓 Panier (temporaire pour le développement)
                        .requestMatchers("/api/cart/**").permitAll()

                        // 🛡️ Back office réservé ADMIN
                        // Note: Assurez-vous que votre UserDetails renvoie bien une autorité nommée "ADMIN"
                        .requestMatchers("/api/admin/**").hasAuthority("ADMIN")

                        // 🔒 Tout le reste nécessite une authentification
                        .anyRequest().authenticated()
                )
                // Ajout du filtre JWT avant le filtre standard username/password
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Configuration CORS pour autoriser le Frontend Angular (localhost:4200)
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        // Autoriser l'origine Angular
        cfg.setAllowedOrigins(List.of("http://localhost:4200"));
        // Autoriser toutes les méthodes HTTP nécessaires
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        // Autoriser les headers (notamment Authorization pour le JWT)
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        // Autoriser l'envoi de credentials (cookies/headers auth)
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}