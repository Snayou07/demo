package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authz -> authz
                        // Публічні ресурси: статика, прото-файл, сторінка логіну
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/match.proto",
                                "/error",
                                "/login",
                                "/oauth2/**"
                        ).permitAll()
                        // Всі інші запити — потребують аутентифікації
                        .anyRequest().authenticated()
                )

                // ✅ Лаб 3: OIDC логін через Casdoor
                // При зверненні до /oauth2/authorization/casdoor — редирект на IAM
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/oauth2/authorization/casdoor")
                        .defaultSuccessUrl("/hello", true)
                        .failureUrl("/login?error=true")
                )

                // ✅ Залишаємо httpBasic для Actuator / API клієнтів
                .httpBasic(withDefaults())

                // ✅ Лаб 3: Повертати 401 замість редиректу для REST API запитів
                // Якщо запит містить заголовок Accept: application/json — повертаємо 401
                .exceptionHandling(ex -> ex
                        .defaultAuthenticationEntryPointFor(
                                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED),
                                request -> {
                                    String accept = request.getHeader("Accept");
                                    return accept != null && accept.contains("application/json");
                                }
                        )
                )

                // ✅ CSRF: використовуємо Cookie-based токен (сумісно з JS fetch)
                // Потрібно для захисту форм, але дозволяємо читати токен з Cookie на фронті
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        // Виключаємо WebSocket ендпоінт з CSRF перевірки
                        .ignoringRequestMatchers("/ws/**")
                );

        return http.build();
    }
}