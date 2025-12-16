package com.enoch.leathercraft.config;

import com.enoch.leathercraft.auth.service.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
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
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth

                        // ✅ Préflight CORS
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // ✅ Swagger / OpenAPI
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html"
                        ).permitAll()

                        // ✅ Auth publique
                        .requestMatchers("/api/auth/**").permitAll()

                        // ✅ fichiers statiques (images, logo, etc.)
                        .requestMatchers("/uploads/**").permitAll()
                        .requestMatchers("/api/uploads/**").permitAll()

                        // ✅ Contact public
                        .requestMatchers(HttpMethod.POST, "/api/contact").permitAll()

                        // ✅ Produits GET public
                        .requestMatchers(HttpMethod.GET, "/api/products/**").permitAll()

                        // ✅ Avis produits : GET public, le reste authentifié
                        .requestMatchers(HttpMethod.GET, "/api/product-reviews/**").permitAll()
                        .requestMatchers("/api/product-reviews/**").authenticated()

                        // ✅ Panier + commandes
                        .requestMatchers("/api/cart/**").hasAnyRole("CUSTOMER", "ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/orders/**").hasAnyRole("CUSTOMER", "ADMIN", "SUPER_ADMIN")

                        // ✅ Admin (FIX: authority explicite)
                        .requestMatchers("/api/admin/**")
                        .hasAnyAuthority("ROLE_ADMIN", "ROLE_SUPER_ADMIN")

                        // ✅ Super-admin partagés (admin + superadmin)
                        .requestMatchers("/api/super-admin/contact-messages/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                        .requestMatchers("/api/super-admin/reactivation-requests/**").hasAnyRole("ADMIN", "SUPER_ADMIN")

                        // ✅ Le reste du super-admin : uniquement super admin
                        .requestMatchers("/api/super-admin/**").hasRole("SUPER_ADMIN")

                        // ✅ Validation coupon public
                        .requestMatchers(HttpMethod.GET, "/api/coupons/validate").permitAll()

                        // ✅ Tout le reste
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
        cfg.setAllowedHeaders(List.of("*"));

        // ✅ important pour télécharger les PDFs (Content-Disposition visible)
        cfg.setExposedHeaders(List.of(HttpHeaders.CONTENT_DISPOSITION));

        cfg.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }
}
