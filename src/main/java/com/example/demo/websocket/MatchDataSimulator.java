package com.example.demo.websocket;

import com.example.demo.proto.MatchUpdate;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.BinaryMessage;
import java.io.IOException;
import java.util.Random;

@Service
@EnableScheduling
public class MatchDataSimulator {

    private final FootballWebSocketHandler handler;
    private final Random random = new Random();

    public MatchDataSimulator(FootballWebSocketHandler handler) {
        this.handler = handler;
    }

    @Scheduled(fixedRate = 5000)
    public void sendUpdate() {
        if (FootballWebSocketHandler.subscriptions.isEmpty()) return;

        // Створюємо бінарне повідомлення через Protobuf
        MatchUpdate update = MatchUpdate.newBuilder()
                .setMatchId("real-barca")
                .setHomeTeam("Real Madrid")
                .setAwayTeam("FC Barcelona")
                .setScore(random.nextInt(3) + ":" + random.nextInt(3))
                .setMinute(random.nextInt(90))
                .setEvent("Action in the match!")
                .build();

        byte[] binaryData = update.toByteArray();

        FootballWebSocketHandler.subscriptions.forEach((session, matchId) -> {
            if (session.isOpen() && matchId.equals(update.getMatchId())) {
                try {
                    session.sendMessage(new BinaryMessage(binaryData));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }
}