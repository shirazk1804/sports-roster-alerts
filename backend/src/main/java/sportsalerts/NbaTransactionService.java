package sportsalerts;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class NbaTransactionService {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String apiKey;

    private final SportradarRequestLimiter requestLimiter;

    public NbaTransactionService(
            ObjectMapper objectMapper,
            @Value("${SPORTRADAR_API_KEY:}") String apiKey,
            SportradarRequestLimiter requestLimiter) {

        this.objectMapper = objectMapper;

        this.apiKey = apiKey;

        this.requestLimiter = requestLimiter;

        this.restClient = RestClient.create(
                "https://api.sportradar.com");
    }

    public String getDailyTransfers(
            LocalDate date) {

        if (apiKey == null ||
                apiKey.isBlank()) {

            throw new IllegalStateException(
                    "Sportradar API key is not configured");
        }

        String year = String.valueOf(
                date.getYear());

        String month = String.format(
                "%02d",
                date.getMonthValue());

        String day = String.format(
                "%02d",
                date.getDayOfMonth());

        return requestLimiter.execute(
                () -> restClient
                        .get()
                        .uri(
                                "/nba/trial/v8/en/league/"
                                        + year
                                        + "/"
                                        + month
                                        + "/"
                                        + day
                                        + "/transfers.json")
                        .header(
                                "x-api-key",
                                apiKey)
                        .accept(
                                MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(
                                String.class));
    }

    public List<NbaTransactionEvent> getNormalizedTransfersForTeam(
            String rawJson,
            String externalProviderTeamId) {

        List<NbaTransactionEvent> events = new ArrayList<>();

        try {

            JsonNode root = objectMapper.readTree(
                    rawJson);

            JsonNode players = root.get(
                    "players");

            if (players == null ||
                    !players.isArray()) {

                return events;
            }

            for (JsonNode player : players) {

                String playerId = getText(
                        player,
                        "id");

                String playerName = getText(
                        player,
                        "full_name");

                JsonNode transfers = player.get(
                        "transfers");

                if (transfers == null ||
                        !transfers.isArray()) {

                    continue;
                }

                for (JsonNode transfer : transfers) {

                    if (!belongsToTeam(
                            transfer,
                            externalProviderTeamId)) {

                        continue;
                    }

                    String transferId = getText(
                            transfer,
                            "id");

                    String description = getText(
                            transfer,
                            "desc");

                    String effectiveDate = getText(
                            transfer,
                            "effective_date");

                    String transactionCode = getText(
                            transfer,
                            "transaction_code");

                    String transactionType = getText(
                            transfer,
                            "transaction_type");

                    String notes = getText(
                            transfer,
                            "notes");

                    String eventType = normalizeEventType(
                            transactionCode,
                            transactionType,
                            description);

                    if (eventType == null) {

                        System.out.println(
                                "Unmapped NBA transaction: "
                                        + transactionCode
                                        + " | "
                                        + transactionType
                                        + " | "
                                        + description);

                        continue;
                    }

                    String teamName = getTeamName(
                            transfer,
                            externalProviderTeamId);

                    if (description.isBlank()) {

                        description = playerName
                                + " - "
                                + transactionType;
                    }

                    events.add(
                            new NbaTransactionEvent(
                                    transferId,
                                    playerId,
                                    playerName,
                                    externalProviderTeamId,
                                    teamName,
                                    eventType,
                                    effectiveDate,
                                    description,
                                    transactionCode,
                                    transactionType,
                                    notes));
                }
            }

            return events;

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Could not parse NBA transfers",
                    exception);
        }
    }

    private boolean belongsToTeam(
            JsonNode transfer,
            String externalProviderTeamId) {

        JsonNode fromTeam = transfer.get(
                "from_team");

        if (teamMatches(
                fromTeam,
                externalProviderTeamId)) {

            return true;
        }

        JsonNode toTeam = transfer.get(
                "to_team");

        return teamMatches(
                toTeam,
                externalProviderTeamId);
    }

    private boolean teamMatches(
            JsonNode team,
            String externalProviderTeamId) {

        if (team == null ||
                team.isNull()) {

            return false;
        }

        String teamId = getText(
                team,
                "id");

        return externalProviderTeamId
                .equals(
                        teamId);
    }

    private String getTeamName(
            JsonNode transfer,
            String externalProviderTeamId) {

        JsonNode toTeam = transfer.get(
                "to_team");

        if (teamMatches(
                toTeam,
                externalProviderTeamId)) {

            return buildTeamName(
                    toTeam);
        }

        JsonNode fromTeam = transfer.get(
                "from_team");

        if (teamMatches(
                fromTeam,
                externalProviderTeamId)) {

            return buildTeamName(
                    fromTeam);
        }

        return "Unknown Team";
    }

    private String buildTeamName(
            JsonNode team) {

        String market = getText(
                team,
                "market");

        String name = getText(
                team,
                "name");

        return (market + " " + name)
                .trim();
    }

    private String normalizeEventType(
            String transactionCode,
            String transactionType,
            String description) {

        String code = transactionCode == null
                ? ""
                : transactionCode
                        .trim()
                        .toUpperCase();

        String type = transactionType == null
                ? ""
                : transactionType
                        .trim()
                        .toLowerCase();

        String desc = description == null
                ? ""
                : description
                        .trim()
                        .toLowerCase();

        /*
         * Trades
         */
        if ("TRD".equals(code) ||
                type.contains("trade") ||
                desc.contains("traded")) {

            return "TRADE";
        }

        /*
         * G League assignment.
         */
        if ("MIN".equals(code) ||
                type.contains(
                        "assigned to minors")
                ||
                desc.contains(
                        "assigned to the g-league")
                ||
                desc.contains(
                        "assigned to the g league")) {

            return "G_LEAGUE_ASSIGNMENT";
        }

        /*
         * G League recall.
         */
        if ("REC".equals(code) ||
                type.contains(
                        "recalled from minors")
                ||
                desc.contains(
                        "recalled from the g-league")
                ||
                desc.contains(
                        "recalled from the g league")) {

            return "G_LEAGUE_RECALL";
        }

        /*
         * Suspensions.
         */
        if ("SUS".equals(code) ||
                "TSUS".equals(code) ||
                type.contains(
                        "suspension")) {

            return "SUSPENDED";
        }

        if ("RSUS".equals(code) ||
                type.contains(
                        "reinstated from suspension")) {

            return "SUSPENSION_REINSTATED";
        }

        /*
         * Waivers / releases.
         */
        if ("WA".equals(code) ||
                type.contains("waiv") ||
                desc.contains("waived")) {

            return "WAIVED";
        }

        if ("REL".equals(code) ||
                "CEXP".equals(code) ||
                type.contains("release") ||
                desc.contains("released")) {

            return "RELEASED";
        }

        /*
         * Signings and contract additions.
         */
        if ("SGN".equals(code) ||
                "RSGN".equals(code) ||
                "10D".equals(code) ||
                "10D2".equals(code) ||
                "CEXT".equals(code) ||
                "CL".equals(code) ||
                type.contains("sign") ||
                desc.contains("signed")) {

            return "SIGNED";
        }

        /*
         * Inactive list.
         */
        if ("INACT".equals(code) ||
                type.contains(
                        "inactive")) {

            return "INACTIVE";
        }

        /*
         * Activated from an inactive/status
         * list.
         */
        if ("ACT".equals(code) ||
                type.contains(
                        "activated")) {

            return "ACTIVATED";
        }

        return null;
    }

    private String getText(
            JsonNode node,
            String field) {

        if (node == null) {
            return "";
        }

        JsonNode value = node.get(
                field);

        if (value == null ||
                value.isNull()) {

            return "";
        }

        return value
                .asString()
                .trim();
    }
}