package sportsalerts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class PushNotificationService {

    private final PushTokenRepository
        pushTokenRepository;

    private final PushReceiptTicketRepository
        pushReceiptTicketRepository;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    public PushNotificationService(
        PushTokenRepository pushTokenRepository,
        PushReceiptTicketRepository pushReceiptTicketRepository,
        ObjectMapper objectMapper
    ) {
        this.pushTokenRepository =
            pushTokenRepository;

        this.pushReceiptTicketRepository =
            pushReceiptTicketRepository;

        this.objectMapper =
            objectMapper;

        this.restClient =
            RestClient.create(
                "https://exp.host"
            );
    }

    public int sendRosterEventNotification(
        Long appUserId,
        RosterEvent event
    ) {
        List<PushToken> pushTokens =
            pushTokenRepository.findByAppUserId(
                appUserId
            );

        int notificationsSent = 0;

        for (PushToken pushToken : pushTokens) {

            try {
                Map<String, Object> data =
                    new HashMap<>();

                data.put(
                    "rosterEventId",
                    event.getId()
                );

                data.put(
                    "teamName",
                    event.getTeamName()
                );

                data.put(
                    "eventType",
                    event.getEventType()
                );

                Map<String, Object> message =
                    new HashMap<>();

                message.put(
                    "to",
                    pushToken.getExpoPushToken()
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
                    data
                );

                String response =
                    restClient
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

                boolean accepted =
                    handlePushTicket(
                        response,
                        pushToken
                    );

                if (accepted) {
                    notificationsSent++;
                }

            } catch (Exception exception) {

                System.err.println(
                    "Could not send push notification: "
                    + exception.getMessage()
                );
            }
        }

        return notificationsSent;
    }

    private boolean handlePushTicket(
        String response,
        PushToken pushToken
    ) throws Exception {

        JsonNode root =
            objectMapper.readTree(response);

        JsonNode data =
            root.get("data");

        if (data == null) {

            System.err.println(
                "Expo push response did not contain a ticket."
            );

            return false;
        }

        JsonNode ticket;

        if (data.isArray()) {

            if (data.size() == 0) {
                return false;
            }

            ticket =
                data.get(0);

        } else {
            ticket =
                data;
        }

        String status =
            getText(
                ticket,
                "status"
            );

        if ("ok".equals(status)) {

            String receiptId =
                getText(
                    ticket,
                    "id"
                );

            if (!receiptId.isBlank()) {

                pushReceiptTicketRepository.save(
                    new PushReceiptTicket(
                        receiptId,
                        pushToken.getExpoPushToken()
                    )
                );

                System.out.println(
                    "Expo push accepted. "
                    + "Receipt saved for later check."
                );
            }

            return true;
        }

        if ("error".equals(status)) {

            String error =
                getNestedError(ticket);

            System.err.println(
                "Expo push ticket error: "
                + error
            );

            if (
                "DeviceNotRegistered"
                    .equals(error)
            ) {
                pushTokenRepository.delete(
                    pushToken
                );

                System.out.println(
                    "Removed invalid push token."
                );
            }
        }

        return false;
    }

    private String getNestedError(
        JsonNode node
    ) {
        JsonNode details =
            node.get("details");

        if (details == null) {
            return "";
        }

        return getText(
            details,
            "error"
        );
    }

    private String getText(
        JsonNode node,
        String field
    ) {
        if (node == null) {
            return "";
        }

        JsonNode value =
            node.get(field);

        if (
            value == null ||
            value.isNull()
        ) {
            return "";
        }

        return value.asString();
    }
}