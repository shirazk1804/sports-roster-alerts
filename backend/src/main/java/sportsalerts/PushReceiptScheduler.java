package sportsalerts;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class PushReceiptScheduler {

    private final PushReceiptTicketRepository
        pushReceiptTicketRepository;

    private final PushTokenRepository
        pushTokenRepository;

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    public PushReceiptScheduler(
        PushReceiptTicketRepository pushReceiptTicketRepository,
        PushTokenRepository pushTokenRepository,
        ObjectMapper objectMapper
    ) {
        this.pushReceiptTicketRepository =
            pushReceiptTicketRepository;

        this.pushTokenRepository =
            pushTokenRepository;

        this.objectMapper =
            objectMapper;

        this.restClient =
            RestClient.create(
                "https://exp.host"
            );
    }

    @Scheduled(
        initialDelay = 60000,
        fixedDelay = 300000
    )
    public void checkPushReceipts() {

        LocalDateTime now =
            LocalDateTime.now();

        LocalDateTime cutoff =
            now.minusMinutes(15);

        List<PushReceiptTicket> tickets =
            pushReceiptTicketRepository
                .findTop1000ByCreatedAtBeforeOrderByCreatedAtAsc(
                    cutoff
                );

        if (tickets.isEmpty()) {
            return;
        }

        List<String> receiptIds =
            tickets
                .stream()
                .map(
                    PushReceiptTicket::getReceiptId
                )
                .toList();

        try {
            String response =
                restClient
                    .post()
                    .uri(
                        "/--/api/v2/push/getReceipts"
                    )
                    .contentType(
                        MediaType.APPLICATION_JSON
                    )
                    .body(
                        Map.of(
                            "ids",
                            receiptIds
                        )
                    )
                    .retrieve()
                    .body(String.class);

            JsonNode root =
                objectMapper.readTree(response);

            JsonNode data =
                root.get("data");

            if (
                data == null ||
                !data.isObject()
            ) {
                System.err.println(
                    "Expo receipt response contained no data."
                );

                return;
            }

            for (
                PushReceiptTicket ticket :
                tickets
            ) {

                JsonNode receipt =
                    data.get(
                        ticket.getReceiptId()
                    );

                if (receipt == null) {

                    // Expo clears receipts after
                    // roughly 24 hours. Don't keep
                    // old unresolved tickets forever.
                    if (
                        ticket
                            .getCreatedAt()
                            .isBefore(
                                now.minusHours(23)
                            )
                    ) {
                        pushReceiptTicketRepository
                            .delete(ticket);
                    }

                    continue;
                }

                processReceipt(
                    ticket,
                    receipt
                );
            }

        } catch (Exception exception) {

            System.err.println(
                "Could not check Expo push receipts: "
                + exception.getMessage()
            );
        }
    }

    private void processReceipt(
        PushReceiptTicket ticket,
        JsonNode receipt
    ) {

        String status =
            getText(
                receipt,
                "status"
            );

        if ("ok".equals(status)) {

            System.out.println(
                "Expo push receipt confirmed."
            );

            pushReceiptTicketRepository
                .delete(ticket);

            return;
        }

        if ("error".equals(status)) {

            String error =
                getNestedError(receipt);

            System.err.println(
                "Expo push receipt error: "
                + error
            );

            if (
                "DeviceNotRegistered"
                    .equals(error)
            ) {

                pushTokenRepository
                    .findByExpoPushToken(
                        ticket.getPushToken()
                    )
                    .ifPresent(
                        pushToken -> {
                            pushTokenRepository
                                .delete(pushToken);

                            System.out.println(
                                "Removed invalid push token."
                            );
                        }
                    );
            }

            pushReceiptTicketRepository
                .delete(ticket);
        }
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