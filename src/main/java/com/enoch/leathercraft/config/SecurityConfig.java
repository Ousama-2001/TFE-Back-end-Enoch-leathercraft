package com.enoch.leathercraft.config;

import com.enoch.leathercraft.auth.service.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
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

    @Bean
    public WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers("/uploads/**");
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth

                        // 1️⃣ Swagger / OpenAPI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // 2️⃣ Auth publique
                        .requestMatchers("/api/auth/**").permitAll()

                        // 3️⃣ Contact (PUBLIC)
                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()

                        // 4️⃣ Produits (GET public)
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // 5️⃣ Avis produits (GET public)
                        .requestMatchers(HttpMethod.GET, "/api/product-reviews/**").permitAll()
                        .requestMatchers("/api/product-reviews/**").authenticated()

                        // 6️⃣ Panier & commandes
                        .requestMatchers("/api/cart/**").authenticated()
                        .requestMatchers("/api/orders/**").authenticated()

                        // 7️⃣ Admin (ADMIN + SUPER_ADMIN)
                        .requestMatchers("/api/admin/**")
                        .hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        // ✅ 8️⃣ Super-admin partagés (ADMIN + SUPER_ADMIN)
                        // IMPORTANT : ces règles doivent être AVANT /api/super-admin/**
                        .requestMatchers("/api/super-admin/contact-messages/**")
                        .hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        .requestMatchers("/api/super-admin/reactivation-requests/**")
                        .hasAnyAuthority("ADMIN", "SUPER_ADMIN")

                        // 🔒 9️⃣ Tout le reste en SUPER_ADMIN only
                        .requestMatchers("/api/super-admin/**")
                        .hasAuthority("SUPER_ADMIN")

                        // 10️⃣ Tout le reste
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable());

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration cfg = new CorsConfiguration();
        cfg.setAllowedOrigins(List.of("http://localhost:4200"));
        cfg.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        cfg.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
