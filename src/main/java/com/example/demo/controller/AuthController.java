package com.example.demo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class AuthController {

    // Лаб 2: простий ендпоінт для перевірки що сервіс живий
    @GetMapping("/hello")
    public String hello() {
        return "Hello from Kolodiuk Bohdan KP-31";
    }

    // ✅ Лаб 3: Повернути claims з JWT токена (інформація про користувача)
    // Spring Security автоматично валідує JWT при oauth2Login — якщо токен невалідний,
    // OidcUser буде null і Spring поверне 401 до того, як метод взагалі викличеться.
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(
            @AuthenticationPrincipal OidcUser oidcUser) {

        if (oidcUser == null) {
            // Цей кейс спрацює якщо хтось зайде через httpBasic, а не OIDC
            return ResponseEntity.status(401).build();
        }

        // getClaims() повертає всі поля з ID Token: sub, email, name, preferred_username і т.д.
        return ResponseEntity.ok(oidcUser.getClaims());
    }

    // ✅ Лаб 3 (п. 4b): Ендпоінт для отримання Access Token — фронт збереже його в Cookie
    @GetMapping("/api/token")
    public ResponseEntity<Map<String, String>> getAccessToken(
            @RegisteredOAuth2AuthorizedClient("casdoor") OAuth2AuthorizedClient authorizedClient) {

        if (authorizedClient == null) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = authorizedClient.getAccessToken().getTokenValue();

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer"
        ));
    }
}