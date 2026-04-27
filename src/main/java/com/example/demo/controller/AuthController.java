package com.example.demo.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
//import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class AuthController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello from Kolodiuk Bohdan KP-31"; // Лаб 2
    }
/*
    @GetMapping("/user-info")
    public Map<String, Object> getUserInfo(@AuthenticationPrincipal OidcUser oidcUser) {
        return oidcUser.getClaims(); // Лаб 3
    }

 */
}

