package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Публічні ресурси
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/match.proto",
                                "/error",
                                "/login",
                                "/auth/callback",   // наш власний callback
                                "/auth/login"       // наш власний login redirect
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                // Вимикаємо вбудований oauth2Login — реалізуємо вручну
                .httpBasic(c -> c.disable())

                // Повертаємо 401 для JSON запитів
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> {
                                    String accept = request.getHeader("Accept");
                                    return accept != null && accept.contains("application/json");
                                }
                        )
                )
                // CSRF
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers("/ws/**", "/auth/**")
                );

        return http.build();
    }
}