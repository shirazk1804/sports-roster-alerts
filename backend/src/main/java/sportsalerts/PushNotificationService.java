package sportsalerts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class PushNotificationService {

    private final PushTokenRepository
        pushTokenRepository;

    private final RestClient
        expoPushClient;

    public PushNotificationService(
        PushTokenRepository pushTokenRepository
    ) {
        this.pushTokenRepository =
            pushTokenRepository;

        this.expoPushClient =
            RestClient.create(
                "https://exp.host"
            );
    }

    public void sendRosterEventNotification(
        RosterEvent event
    ) {
        List<PushToken> tokens =
            pushTokenRepository.findAll();

        for (PushToken token : tokens) {
            try {
                Map<String, Object> message =
                    new LinkedHashMap<>();

                message.put(
                    "to",
                    token.getExpoPushToken()
                );

                message.put(
                    "sound",
                    "default"
                );

                message.put(
                    "title",
                    event.getTeamName()
                    + ": "
                    + event.getPlayerName()
                );

                message.put(
                    "body",
                    event.getDescription()
                );

                message.put(
                    "data",
                    Map.of(
                        "rosterEventId",
                        event.getId(),
                        "teamName",
                        event.getTeamName(),
                        "eventType",
                        event.getEventType()
                    )
                );

                String response =
                    expoPushClient
                        .post()
                        .uri(
                            "/--/api/v2/push/send"
                        )
                        .contentType(
                            MediaType.APPLICATION_JSON
                        )
                        .body(message)
                        .retrieve()
                        .body(String.class);

                System.out.println(
                    "Push sent for "
                    + event.getPlayerName()
                    + ": "
                    + response
                );

            } catch (Exception exception) {
                System.err.println(
                    "Push failed for "
                    + event.getPlayerName()
                    + ": "
                    + exception.getMessage()
                );
            }
        }
    }
}