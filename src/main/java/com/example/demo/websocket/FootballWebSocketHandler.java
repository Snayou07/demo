package com.example.demo.websocket;

import com.example.demo.proto.MatchSubscription; // Це згенерований клас
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.BinaryWebSocketHandler;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class FootballWebSocketHandler extends BinaryWebSocketHandler {

    public static final Map<WebSocketSession, String> subscriptions = new ConcurrentHashMap<>();

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        byte[] payload = message.getPayload().array();
        // Використовуємо згенерований Protobuf клас
        MatchSubscription sub = MatchSubscription.parseFrom(payload);
        subscriptions.put(session, sub.getMatchId());
        System.out.println("Юзер підписався на матч: " + sub.getMatchId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        subscriptions.remove(session);
    }
}