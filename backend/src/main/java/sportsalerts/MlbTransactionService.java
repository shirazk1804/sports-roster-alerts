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
            ObjectMapper objectMapper) {
        this.restClient = RestClient.create(
                "https://statsapi.mlb.com");

        this.objectMapper = objectMapper;
    }

    public String getTransactions(
            Long teamId,
            String startDate,
            String endDate) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("teamId", teamId)
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .build())
                .retrieve()
                .body(String.class);
    }

    public String getAllMlbTransactions(
            String startDate,
            String endDate) {
        return restClient
                .get()
                .uri(uriBuilder -> uriBuilder
                        .path("/api/v1/transactions")
                        .queryParam("startDate", startDate)
                        .queryParam("endDate", endDate)
                        .queryParam("sportId", 1)
                        .build())
                .retrieve()
                .body(String.class);
    }

    public List<MlbTransactionEvent> getNormalizedTransactions(
            Long teamId,
            String startDate,
            String endDate) {

        String rawJson = getTransactions(
                teamId,
                startDate,
                endDate);

        return getNormalizedTransactionsForTeam(
                rawJson,
                teamId);
    }

    public List<MlbTransactionEvent> getNormalizedTransactionsForTeam(
            String rawJson,
            Long teamId) {

        List<MlbTransactionEvent> events = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(rawJson);

            JsonNode transactions = root.get("transactions");

            if (transactions == null ||
                    !transactions.isArray()) {
                return events;
            }

            for (JsonNode transaction : transactions) {
                if (!belongsToTeam(
                        transaction,
                        teamId)) {
                    continue;
                }

                String typeCode = getText(
                        transaction,
                        "typeCode");

                String description = getText(
                        transaction,
                        "description");

                String eventType = normalizeEventType(
                        typeCode,
                        description);

                if (eventType == null) {
                    continue;
                }

                JsonNode person = transaction.get("person");

                Long sourceTransactionId = getLong(
                        transaction,
                        "id");

                Long playerId = person == null
                        ? null
                        : getLong(
                                person,
                                "id");

                String playerName = person == null
                        ? "Unknown Player"
                        : getText(
                                person,
                                "fullName");

                String teamName = getTeamName(
                        transaction,
                        teamId);

                String date = getText(
                        transaction,
                        "date");

                events.add(
                        new MlbTransactionEvent(
                                sourceTransactionId,
                                playerId,
                                playerName,
                                teamName,
                                eventType,
                                date,
                                description));
            }

            return events;

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Could not parse MLB transactions",
                    exception);
        }
    }

    private boolean belongsToTeam(
            JsonNode transaction,
            Long teamId) {
        JsonNode toTeam = transaction.get("toTeam");

        if (toTeam != null) {
            Long toTeamId = getLong(
                    toTeam,
                    "id");

            if (teamId.equals(toTeamId)) {
                return true;
            }
        }

        JsonNode fromTeam = transaction.get("fromTeam");

        if (fromTeam != null) {
            Long fromTeamId = getLong(
                    fromTeam,
                    "id");

            if (teamId.equals(fromTeamId)) {
                return true;
            }
        }

        return false;
    }

    private String getTeamName(
            JsonNode transaction,
            Long teamId) {
        JsonNode toTeam = transaction.get("toTeam");

        if (toTeam != null) {
            Long toTeamId = getLong(
                    toTeam,
                    "id");

            if (teamId.equals(toTeamId)) {
                return getText(
                        toTeam,
                        "name");
            }
        }

        JsonNode fromTeam = transaction.get("fromTeam");

        if (fromTeam != null) {
            Long fromTeamId = getLong(
                    fromTeam,
                    "id");

            if (teamId.equals(fromTeamId)) {
                return getText(
                        fromTeam,
                        "name");
            }
        }

        return "Unknown Team";
    }

    private String normalizeEventType(
            String typeCode,
            String description) {
        String code = typeCode == null
                ? ""
                : typeCode.toUpperCase();

        String desc = description == null
                ? ""
                : description.toLowerCase();

        // Bereavement list
        if (desc.contains("bereavement list")) {
            if (desc.contains("placed") ||
                    code.equals("BRV")) {
                return "BEREAVEMENT_PLACEMENT";
            }

            if (desc.contains("activated") ||
                    desc.contains("reinstated") ||
                    code.equals("RBL")) {
                return "BEREAVEMENT_ACTIVATION";
            }
        }

        // Paternity list
        if (desc.contains("paternity list")) {
            if (desc.contains("placed") ||
                    code.equals("PCT")) {
                return "PATERNITY_PLACEMENT";
            }

            if (desc.contains("activated") ||
                    desc.contains("reinstated") ||
                    code.equals("RPC")) {
                return "PATERNITY_ACTIVATION";
            }
        }

        // Restricted list
        if (desc.contains("restricted list")) {
            if (desc.contains("placed")) {
                return "RESTRICTED_LIST_PLACEMENT";
            }

            if (desc.contains("activated") ||
                    desc.contains("reinstated")) {
                return "RESTRICTED_LIST_ACTIVATION";
            }
        }

        // Injured list transfers must be checked
        // before normal IL placements.
        if (desc.contains("transferred") &&
                desc.contains("injured list")) {
            return "IL_TRANSFER";
        }

        if (desc.contains("placed") &&
                desc.contains("injured list")) {
            return "IL_PLACEMENT";
        }

        if ((desc.contains("activated") ||
                desc.contains("reinstated")) &&
                desc.contains("injured list")) {
            return "IL_ACTIVATION";
        }

        // Suspensions
        if (desc.contains("reinstated") &&
                desc.contains("suspension")) {
            return "SUSPENSION_REINSTATED";
        }

        if (desc.contains("suspended") ||
                desc.contains("suspension")) {
            return "SUSPENDED";
        }

        // Waivers
        if (desc.contains("claimed") &&
                desc.contains("waiver")) {
            return "WAIVER_CLAIM";
        }

        if (desc.contains("placed") &&
                desc.contains("waiver")) {
            return "WAIVERS";
        }

        // Releases / signings
        if (desc.contains("released")) {
            return "RELEASED";
        }

        if (desc.contains("signed") &&
                desc.contains("minor league")) {
            return "MINOR_LEAGUE_SIGNING";
        }

        if (desc.contains("signed") &&
                (desc.contains("free agent") ||
                        desc.contains("contract"))) {
            return "SIGNED";
        }

        // Standard roster moves
        if (desc.contains("designated for assignment")) {
            return "DESIGNATED_FOR_ASSIGNMENT";
        }

        if (desc.contains("recalled")) {
            return "RECALLED";
        }

        if (desc.contains("optioned")) {
            return "OPTIONED";
        }

        if (desc.contains("selected the contract")) {
            return "CONTRACT_SELECTED";
        }

        if (desc.contains("outright")) {
            return "OUTRIGHTED";
        }

        if (desc.contains("rehab assignment")) {
            return "REHAB_ASSIGNMENT";
        }

        if (desc.contains("traded")) {
            return "TRADE";
        }

        if (desc.contains("retired")) {
            return "RETIRED";
        }

        // Some transactions simply say "activated"
        // without identifying a particular list.
        if (desc.contains("activated")) {
            return "ROSTER_ACTIVATION";
        }

        // Code fallbacks.
        // Keep our existing codes and support additional
        // MLB transaction codes.
        return switch (code) {

            case "BRV" ->
                "BEREAVEMENT_PLACEMENT";

            case "RBL" ->
                "BEREAVEMENT_ACTIVATION";

            case "PCT" ->
                "PATERNITY_PLACEMENT";

            case "RPC" ->
                "PATERNITY_ACTIVATION";

            case "CU", "SU" ->
                "RECALLED";

            case "OPT", "OA" ->
                "OPTIONED";

            case "DES" ->
                "DESIGNATED_FOR_ASSIGNMENT";

            case "TE", "TR" ->
                "TRADE";

            case "SE", "PUR" ->
                "CONTRACT_SELECTED";

            case "OUT" ->
                "OUTRIGHTED";

            case "ASG" ->
                "REHAB_ASSIGNMENT";

            case "IL" ->
                "IL_PLACEMENT";

            case "ACT" ->
                "IL_ACTIVATION";

            case "RE" ->
                "RELEASED";

            case "WV" ->
                "WAIVERS";

            case "MIN" ->
                "MINOR_LEAGUE_SIGNING";

            case "RET" ->
                "RETIRED";

            default ->
                null;
        };
    }

    private String getText(
            JsonNode node,
            String field) {
        JsonNode value = node.get(field);

        if (value == null ||
                value.isNull()) {
            return "";
        }

        return value.asString();
    }

    private Long getLong(
            JsonNode node,
            String field) {
        JsonNode value = node.get(field);

        if (value == null ||
                value.isNull()) {
            return null;
        }

        return value.asLong();
    }
}