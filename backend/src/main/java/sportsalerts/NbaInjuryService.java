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
public class NbaInjuryService {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String apiKey;

    private final SportradarRequestLimiter
            requestLimiter;

    public NbaInjuryService(
            ObjectMapper objectMapper,
            @Value("${SPORTRADAR_API_KEY:}")
            String apiKey,
            SportradarRequestLimiter requestLimiter) {

        this.objectMapper =
                objectMapper;

        this.apiKey =
                apiKey;

        this.requestLimiter =
                requestLimiter;

        this.restClient =
                RestClient.create(
                        "https://api.sportradar.com");
    }

    public String getLeagueInjuries() {

        ensureApiKey();

        return requestLimiter.execute(
                () -> restClient
                        .get()
                        .uri(
                                "/nba/trial/v8/en/league/injuries.json")
                        .header(
                                "x-api-key",
                                apiKey)
                        .accept(
                                MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(
                                String.class));
    }

    public List<NbaInjuryEvent>
            getNormalizedInjuriesForTeam(
                    String rawJson,
                    String externalProviderTeamId) {

        List<NbaInjuryEvent> events =
                new ArrayList<>();

        try {

            JsonNode root =
                    objectMapper.readTree(
                            rawJson);

            JsonNode teams =
                    root.get(
                            "teams");

            if (teams == null ||
                    !teams.isArray()) {

                return events;
            }

            for (
                    JsonNode team :
                    teams) {

                String teamId =
                        getText(
                                team,
                                "id");

                if (!externalProviderTeamId
                        .equals(
                                teamId)) {

                    continue;
                }

                String market =
                        getText(
                                team,
                                "market");

                String name =
                        getText(
                                team,
                                "name");

                String teamName =
                        (market + " " + name)
                                .trim();

                JsonNode players =
                        team.get(
                                "players");

                if (players == null ||
                        !players.isArray()) {

                    continue;
                }

                for (
                        JsonNode player :
                        players) {

                    String playerId =
                            getText(
                                    player,
                                    "id");

                    String playerName =
                            getText(
                                    player,
                                    "full_name");

                    /*
                     * Prefer NBA-style primary
                     * positions such as PG, SG,
                     * SF, PF and C.
                     */
                    String position =
                            getText(
                                    player,
                                    "primary_position");

                    if (position.isBlank()) {

                        position =
                                getText(
                                        player,
                                        "position");
                    }

                    JsonNode injuries =
                            player.get(
                                    "injuries");

                    if (injuries == null ||
                            !injuries.isArray()) {

                        continue;
                    }

                    for (
                            JsonNode injury :
                            injuries) {

                        String injuryId =
                                getText(
                                        injury,
                                        "id");

                        String description =
                                getText(
                                        injury,
                                        "desc");

                        String status =
                                getText(
                                        injury,
                                        "status");

                        String comment =
                                getText(
                                        injury,
                                        "comment");

                        String startDate =
                                getText(
                                        injury,
                                        "start_date");

                        String updateDate =
                                getText(
                                        injury,
                                        "update_date");

                        if (playerId.isBlank() ||
                                injuryId.isBlank()) {

                            continue;
                        }

                        events.add(
                                new NbaInjuryEvent(
                                        teamId,
                                        teamName,
                                        playerId,
                                        playerName,
                                        position,
                                        injuryId,
                                        description,
                                        status,
                                        comment,
                                        startDate,
                                        updateDate));
                    }
                }
            }

            return events;

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Could not parse NBA injuries",
                    exception);
        }
    }

    private void ensureApiKey() {

        if (apiKey == null ||
                apiKey.isBlank()) {

            throw new IllegalStateException(
                    "Sportradar API key is not configured");
        }
    }

    private String getText(
            JsonNode node,
            String field) {

        if (node == null) {
            return "";
        }

        JsonNode value =
                node.get(
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