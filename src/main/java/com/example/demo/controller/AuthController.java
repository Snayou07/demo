package com.example.demo.controller;

import com.example.demo.service.OidcService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping
public class AuthController {

    private final OidcService oidcService;

    public AuthController(OidcService oidcService) {
        this.oidcService = oidcService;
    }

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Kolodiuk Bohdan KP-31";
    }

    /**
     * Крок 1: Ініціюємо OIDC логін — редирект на Casdoor
     * Аналог /oauth2/authorization/casdoor, але реалізований вручну
     */
    @GetMapping("/auth/login")
    public void login(HttpServletRequest request,
                      HttpServletResponse response) throws Exception {
        // Генеруємо state для захисту від CSRF
        String state = oidcService.generateState();
        // Зберігаємо state в сесії для перевірки на callback
        request.getSession().setAttribute("oidc_state", state);

        String authUrl = oidcService.buildAuthorizationUrl(state);
        response.sendRedirect(authUrl);
    }

    /**
     * Крок 2: Callback від Casdoor з authorization_code
     * Обмінюємо code на токени, валідуємо JWT, зберігаємо в сесії
     */
    @GetMapping("/auth/callback")
    public void callback(
            @RequestParam("code") String code,
            @RequestParam("state") String state,
            HttpServletRequest request,
            HttpServletResponse response) throws Exception {

        // Перевіряємо state (захист від CSRF)
        HttpSession session = request.getSession();
        String savedState = (String) session.getAttribute("oidc_state");
        if (savedState == null || !savedState.equals(state)) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid state parameter");
            return;
        }

        // Обмінюємо code на токени (голий HTTP POST)
        Map<String, Object> tokens = oidcService.exchangeCodeForTokens(code);
        String idToken = (String) tokens.get("id_token");
        String accessToken = (String) tokens.get("access_token");

        if (idToken == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "No id_token received");
            return;
        }

        // Валідуємо підпис JWT вручну через JWK Set
        Claims claims = oidcService.validateAndExtractClaims(idToken);

        // Зберігаємо claims і токени в сесії
        session.setAttribute("oidc_claims", claims);
        session.setAttribute("access_token", accessToken);
        session.setAttribute("id_token", idToken);

        // Редирект на головну після успішного логіну
        response.sendRedirect("/index.html");
    }

    /**
     * Крок 3: Повернути claims з провалідованого JWT токена
     * Якщо сесія не містить claims — 401
     */
    @GetMapping("/user-info")
    public ResponseEntity<Map<String, Object>> getUserInfo(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).build();
        }

        Claims claims = (Claims) session.getAttribute("oidc_claims");
        if (claims == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(claims);
    }

    /**
     * Повернути access token (зберігається у Cookie на фронті)
     */
    @GetMapping("/api/token")
    public ResponseEntity<Map<String, String>> getAccessToken(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) {
            return ResponseEntity.status(401).build();
        }

        String accessToken = (String) session.getAttribute("access_token");
        if (accessToken == null) {
            return ResponseEntity.status(401).build();
        }

        return ResponseEntity.ok(Map.of(
                "access_token", accessToken,
                "token_type", "Bearer"
        ));
    }

    /**
     * Logout — очищаємо сесію
     */
    @GetMapping("/logout")
    public void logout(HttpServletRequest request, HttpServletResponse response) throws Exception {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        response.sendRedirect("/index.html");
    }
}