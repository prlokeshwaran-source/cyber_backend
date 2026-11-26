package com.example.cyber.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.core.annotation.Order;

import java.util.List;

@Configuration
public class SecurityConfiguration {
    @Bean
    @Order(1)
    SecurityFilterChain publicPagesSecurityFilterChain(HttpSecurity http) throws Exception {
        return http
                .securityMatcher("/", "/error", "/favicon.ico")
                .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173,https://cyber-frontend-nu.vercel.app/}") List<String> allowedOrigins) {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(allowedOrigins.stream()
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .map(origin -> origin.replaceAll("/+$", ""))
                .toList());
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Idempotency-Key", "X-Device-Id"));
        configuration.setAllowCredentials(false);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", configuration);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                        .requestMatchers("/", "/error", "/favicon.ico").permitAll()
                        .requestMatchers("/api/v1/auth/register", "/api/v1/security/url-checks",
                                "/api/v1/security/email-checks",
                                "/api/v1/security/transaction-signing-key").permitAll()
                        .anyRequest().authenticated())
                .httpBasic(basic -> basic.authenticationEntryPoint((request, response, exception) -> {
                    response.setStatus(org.springframework.http.HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(org.springframework.http.MediaType.APPLICATION_PROBLEM_JSON_VALUE);
                    response.setCharacterEncoding("UTF-8");
                    response.getWriter().write("{\"type\":\"about:blank\",\"title\":\"Unauthorized\","
                            + "\"status\":401,\"detail\":\"Valid HTTP Basic credentials are required\"}");
                }))
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
