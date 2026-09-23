package sportsalerts;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class NflInjuryService {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String apiKey;

    public NflInjuryService(
        ObjectMapper objectMapper,
        @Value("${SPORTRADAR_API_KEY:}")
        String apiKey
    ) {
        this.objectMapper =
            objectMapper;

        this.apiKey =
            apiKey;

        this.restClient =
            RestClient.create(
                "https://api.sportradar.com"
            );
    }

    public NflWeekInfo getCurrentWeek() {

        ensureApiKey();

        String response =
            restClient
                .get()
                .uri(
                    "/nfl/official/trial/v7/en/games/current_week/schedule.json"
                )
                .header(
                    "x-api-key",
                    apiKey
                )
                .accept(
                    MediaType.APPLICATION_JSON
                )
                .retrieve()
                .body(String.class);

        try {
            JsonNode root =
                objectMapper.readTree(
                    response
                );

            int seasonYear =
                root.get("year")
                    .asInt();

            String seasonType =
                root.get("type")
                    .asString();

            JsonNode weekNode =
                root.get("week");

            if (weekNode == null) {
                throw new IllegalStateException(
                    "Current NFL schedule did not contain week information"
                );
            }

            int week =
                weekNode
                    .get("sequence")
                    .asInt();

            return new NflWeekInfo(
                seasonYear,
                seasonType,
                week
            );

        } catch (Exception exception) {

            throw new RuntimeException(
                "Could not determine current NFL week",
                exception
            );
        }
    }

    public String getWeeklyInjuries(
        NflWeekInfo week
    ) {

        ensureApiKey();

        return restClient
            .get()
            .uri(
                "/nfl/official/trial/v7/en/seasons/"
                    + week.seasonYear()
                    + "/"
                    + week.seasonType()
                    + "/"
                    + week.week()
                    + "/injuries.json"
            )
            .header(
                "x-api-key",
                apiKey
            )
            .accept(
                MediaType.APPLICATION_JSON
            )
            .retrieve()
            .body(String.class);
    }

    public List<NflInjuryEvent>
        getNormalizedInjuriesForTeam(
            String rawJson,
            String externalProviderTeamId
        ) {

        List<NflInjuryEvent> events =
            new ArrayList<>();

        try {
            JsonNode root =
                objectMapper.readTree(
                    rawJson
                );

            JsonNode teams =
                root.get("teams");

            if (
                teams == null ||
                !teams.isArray()
            ) {
                return events;
            }

            for (JsonNode team : teams) {

                String teamId =
                    getText(
                        team,
                        "id"
                    );

                if (
                    !externalProviderTeamId
                        .equals(teamId)
                ) {
                    continue;
                }

                String market =
                    getText(
                        team,
                        "market"
                    );

                String name =
                    getText(
                        team,
                        "name"
                    );

                String teamName =
                    (market + " " + name)
                        .trim();

                JsonNode players =
                    team.get("players");

                if (
                    players == null ||
                    !players.isArray()
                ) {
                    continue;
                }

                for (
                    JsonNode player :
                    players
                ) {
                    String playerId =
                        getText(
                            player,
                            "id"
                        );

                    String playerName =
                        getText(
                            player,
                            "name"
                        );

                    JsonNode injuries =
                        player.get(
                            "injuries"
                        );

                    if (
                        injuries == null ||
                        !injuries.isArray()
                    ) {
                        continue;
                    }

                    for (
                        JsonNode injury :
                        injuries
                    ) {
                        String primary =
                            getText(
                                injury,
                                "primary"
                            );

                        String secondary =
                            getText(
                                injury,
                                "secondary"
                            );

                        String gameStatus =
                            getText(
                                injury,
                                "status"
                            );

                        String statusDate =
                            getText(
                                injury,
                                "status_date"
                            );

                        String estimatedReturnDate =
                            getText(
                                injury,
                                "estimated_return_date"
                            );

                        String practiceStatus =
                            "";

                        JsonNode practice =
                            injury.get(
                                "practice"
                            );

                        if (
                            practice != null &&
                            !practice.isNull()
                        ) {
                            practiceStatus =
                                getText(
                                    practice,
                                    "status"
                                );
                        }

                        String description =
                            buildDescription(
                                playerName,
                                primary,
                                secondary,
                                gameStatus,
                                practiceStatus
                            );

                        events.add(
                            new NflInjuryEvent(
                                teamId,
                                teamName,
                                playerId,
                                playerName,
                                primary,
                                secondary,
                                gameStatus,
                                practiceStatus,
                                statusDate,
                                estimatedReturnDate,
                                description
                            )
                        );
                    }
                }
            }

            return events;

        } catch (Exception exception) {

            throw new RuntimeException(
                "Could not parse NFL injuries",
                exception
            );
        }
    }

    private String buildDescription(
        String playerName,
        String primary,
        String secondary,
        String gameStatus,
        String practiceStatus
    ) {
        StringBuilder description =
            new StringBuilder();

        description.append(
            playerName
        );

        if (!primary.isBlank()) {
            description
                .append(" - ")
                .append(primary);
        }

        if (!secondary.isBlank()) {
            description
                .append(" / ")
                .append(secondary);
        }

        if (!gameStatus.isBlank()) {
            description
                .append(" - ")
                .append(gameStatus);
        }

        if (!practiceStatus.isBlank()) {
            description
                .append(" - ")
                .append(practiceStatus);
        }

        return description.toString();
    }

    private void ensureApiKey() {

        if (
            apiKey == null ||
            apiKey.isBlank()
        ) {
            throw new IllegalStateException(
                "Sportradar API key is not configured"
            );
        }
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