package com.example.demo.config;

import com.example.demo.websocket.FootballWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final FootballWebSocketHandler handler;

    public WebSocketConfig(FootballWebSocketHandler handler) {
        this.handler = handler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        // Дозволяємо підключення до нашого сокета
        registry.addHandler(handler, "/ws/football").setAllowedOrigins("*");
    }
}