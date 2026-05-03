package com.example.demo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OidcService {

    @Value("${casdoor.client-id}")
    private String clientId;

    @Value("${casdoor.client-secret}")
    private String clientSecret;

    @Value("${casdoor.redirect-uri}")
    private String redirectUri;

    @Value("${casdoor.authorization-uri}")
    private String authorizationUri;

    @Value("${casdoor.token-uri}")
    private String tokenUri;

    @Value("${casdoor.jwk-set-uri}")
    private String jwkSetUri;

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Крок 1: Будуємо URL для редиректу на Casdoor
     */
    public String buildAuthorizationUrl(String state) {
        return authorizationUri +
                "?response_type=authorization_code" +
                "&client_id=" + encode(clientId) +
                "&redirect_uri=" + encode(redirectUri) +
                "&scope=" + encode("openid profile email") +
                "&state=" + encode(state);
    }

    /**
     * Крок 2: Обмінюємо authorization_code на токени через прямий HTTP POST
     */
    public Map<String, Object> exchangeCodeForTokens(String code) throws Exception {
        String body = "grant_type=authorization_code" +
                "&code=" + encode(code) +
                "&redirect_uri=" + encode(redirectUri) +
                "&client_id=" + encode(clientId) +
                "&client_secret=" + encode(clientSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUri))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        return objectMapper.readValue(response.body(), Map.class);
    }

    /**
     * Крок 3: Ручна валідація підпису JWT через JWK Set з Casdoor
     * Парсимо header → знаходимо kid → беремо публічний ключ → верифікуємо підпис
     */
    public Claims validateAndExtractClaims(String idToken) throws Exception {
        // 1. Декодуємо header JWT щоб знайти kid (key id)
        String[] parts = idToken.split("\\.");
        if (parts.length != 3) {
            throw new IllegalArgumentException("Невалідний формат JWT");
        }

        String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
        Map<String, Object> header = objectMapper.readValue(headerJson, Map.class);
        String kid = (String) header.get("kid");

        // 2. Отримуємо JWK Set з Casdoor (голий HTTP запит)
        PublicKey publicKey = fetchPublicKey(kid);

        // 3. Валідуємо підпис і повертаємо claims
        return Jwts.parserBuilder()
                .setSigningKey(publicKey)
                .build()
                .parseClaimsJws(idToken)
                .getBody();
    }

    /**
     * Отримуємо публічний ключ з JWK Set endpoint Casdoor
     */
    @SuppressWarnings("unchecked")
    private PublicKey fetchPublicKey(String kid) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(jwkSetUri))
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        Map<String, Object> jwkSet = objectMapper.readValue(response.body(), Map.class);
        List<Map<String, Object>> keys = (List<Map<String, Object>>) jwkSet.get("keys");

        // Знаходимо ключ за kid
        Map<String, Object> jwk = keys.stream()
                .filter(k -> kid == null || kid.equals(k.get("kid")))
                .findFirst()
                .orElse(keys.get(0)); // якщо kid немає — беремо перший

        // Будуємо RSA PublicKey з n та e компонент JWK
        byte[] nBytes = Base64.getUrlDecoder().decode((String) jwk.get("n"));
        byte[] eBytes = Base64.getUrlDecoder().decode((String) jwk.get("e"));

        BigInteger modulus = new BigInteger(1, nBytes);
        BigInteger exponent = new BigInteger(1, eBytes);

        RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(spec);
    }

    public String generateState() {
        return UUID.randomUUID().toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}