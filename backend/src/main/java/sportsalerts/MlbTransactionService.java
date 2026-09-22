package sportsalerts;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class MlbTransactionService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public MlbTransactionService(
        ObjectMapper objectMapper
    ) {
        this.restClient = RestClient.create(
            "https://statsapi.mlb.com"
        );

        this.objectMapper = objectMapper;
    }

    public String getTransactions(
        Long teamId,
        String startDate,
        String endDate
    ) {
        return restClient
            .get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/api/v1/transactions")
                    .queryParam("teamId", teamId)
                    .queryParam("startDate", startDate)
                    .queryParam("endDate", endDate)
                    .build()
            )
            .retrieve()
            .body(String.class);
    }

    public String getAllMlbTransactions(
        String startDate,
        String endDate
    ) {
        return restClient
            .get()
            .uri(uriBuilder ->
                uriBuilder
                    .path("/api/v1/transactions")
                    .queryParam("startDate", startDate)
                    .queryParam("endDate", endDate)
                    .queryParam("sportId", 1)
                    .build()
            )
            .retrieve()
            .body(String.class);
    }

    public List<MlbTransactionEvent>
        getNormalizedTransactions(
            Long teamId,
            String startDate,
            String endDate
        ) {

        String rawJson =
            getTransactions(
                teamId,
                startDate,
                endDate
            );

        return getNormalizedTransactionsForTeam(
            rawJson,
            teamId
        );
    }

    public List<MlbTransactionEvent>
        getNormalizedTransactionsForTeam(
            String rawJson,
            Long teamId
        ) {

        List<MlbTransactionEvent> events =
            new ArrayList<>();

        try {
            JsonNode root =
                objectMapper.readTree(rawJson);

            JsonNode transactions =
                root.get("transactions");

            if (
                transactions == null ||
                !transactions.isArray()
            ) {
                return events;
            }

            for (
                JsonNode transaction :
                transactions
            ) {
                if (
                    !belongsToTeam(
                        transaction,
                        teamId
                    )
                ) {
                    continue;
                }

                String typeCode =
                    getText(
                        transaction,
                        "typeCode"
                    );

                String description =
                    getText(
                        transaction,
                        "description"
                    );

                String eventType =
                    normalizeEventType(
                        typeCode,
                        description
                    );

                if (eventType == null) {
                    continue;
                }

                JsonNode person =
                    transaction.get("person");

                Long sourceTransactionId =
                    getLong(
                        transaction,
                        "id"
                    );

                Long playerId =
                    person == null
                        ? null
                        : getLong(
                            person,
                            "id"
                        );

                String playerName =
                    person == null
                        ? "Unknown Player"
                        : getText(
                            person,
                            "fullName"
                        );

                String teamName =
                    getTeamName(
                        transaction,
                        teamId
                    );

                String date =
                    getText(
                        transaction,
                        "date"
                    );

                events.add(
                    new MlbTransactionEvent(
                        sourceTransactionId,
                        playerId,
                        playerName,
                        teamName,
                        eventType,
                        date,
                        description
                    )
                );
            }

            return events;

        } catch (Exception exception) {
            throw new RuntimeException(
                "Could not parse MLB transactions",
                exception
            );
        }
    }

    private boolean belongsToTeam(
        JsonNode transaction,
        Long teamId
    ) {
        JsonNode toTeam =
            transaction.get("toTeam");

        if (toTeam != null) {
            Long toTeamId =
                getLong(
                    toTeam,
                    "id"
                );

            if (teamId.equals(toTeamId)) {
                return true;
            }
        }

        JsonNode fromTeam =
            transaction.get("fromTeam");

        if (fromTeam != null) {
            Long fromTeamId =
                getLong(
                    fromTeam,
                    "id"
                );

            if (teamId.equals(fromTeamId)) {
                return true;
            }
        }

        return false;
    }

    private String getTeamName(
        JsonNode transaction,
        Long teamId
    ) {
        JsonNode toTeam =
            transaction.get("toTeam");

        if (toTeam != null) {
            Long toTeamId =
                getLong(
                    toTeam,
                    "id"
                );

            if (teamId.equals(toTeamId)) {
                return getText(
                    toTeam,
                    "name"
                );
            }
        }

        JsonNode fromTeam =
            transaction.get("fromTeam");

        if (fromTeam != null) {
            Long fromTeamId =
                getLong(
                    fromTeam,
                    "id"
                );

            if (teamId.equals(fromTeamId)) {
                return getText(
                    fromTeam,
                    "name"
                );
            }
        }

        return "Unknown Team";
    }

    private String normalizeEventType(
        String typeCode,
        String description
    ) {
        String lowerDescription =
            description == null
                ? ""
                : description.toLowerCase();

        if (
            "SC".equals(typeCode) &&
            lowerDescription.contains(
                "placed"
            ) &&
            lowerDescription.contains(
                "injured list"
            )
        ) {
            return "IL_PLACEMENT";
        }

        if (
            "SC".equals(typeCode) &&
            lowerDescription.contains(
                "activated"
            ) &&
            lowerDescription.contains(
                "injured list"
            )
        ) {
            return "IL_ACTIVATION";
        }

        if (
            "SC".equals(typeCode) &&
            lowerDescription.contains(
                "transferred"
            ) &&
            lowerDescription.contains(
                "injured list"
            )
        ) {
            return "IL_TRANSFER";
        }

        return switch (typeCode) {
            case "CU" ->
                "RECALLED";

            case "OPT" ->
                "OPTIONED";

            case "DES" ->
                "DESIGNATED_FOR_ASSIGNMENT";

            case "TR" ->
                "TRADE";

            case "SE" ->
                "CONTRACT_SELECTED";

            case "OUT" ->
                "OUTRIGHTED";

            case "ASG" ->
                "REHAB_ASSIGNMENT";

            default ->
                null;
        };
    }

    private String getText(
        JsonNode node,
        String field
    ) {
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

    private Long getLong(
        JsonNode node,
        String field
    ) {
        JsonNode value =
            node.get(field);

        if (
            value == null ||
            value.isNull()
        ) {
            return null;
        }

        return value.asLong();
    }
}