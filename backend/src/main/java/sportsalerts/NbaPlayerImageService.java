package sportsalerts;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class NbaPlayerImageService {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private volatile Map<String, String>
        espnTeamIdsByName =
            Map.of();

    private final Map<
        String,
        Map<String, String>
    > headshotsByTeam =
        new ConcurrentHashMap<>();

    public NbaPlayerImageService(
        ObjectMapper objectMapper
    ) {
        this.objectMapper =
            objectMapper;

        this.restClient =
            RestClient.create(
                "https://site.api.espn.com"
            );
    }

    public String getHeadshotUrl(
        String teamName,
        String playerName
    ) {

        if (
            teamName == null ||
            teamName.isBlank() ||
            playerName == null ||
            playerName.isBlank()
        ) {
            return null;
        }

        try {

            ensureTeamIdsLoaded();

            String normalizedTeam =
                normalizeName(
                    teamName
                );

            Map<String, String>
                teamHeadshots =
                    headshotsByTeam
                        .computeIfAbsent(
                            normalizedTeam,
                            ignored ->
                                loadTeamRoster(
                                    teamName
                                )
                        );

            return teamHeadshots.get(
                normalizeName(
                    playerName
                )
            );

        } catch (Exception exception) {

            System.err.println(
                "Could not resolve NBA headshot for "
                    + playerName
                    + ": "
                    + exception.getMessage()
            );

            return null;
        }
    }

    private synchronized void
        ensureTeamIdsLoaded() {

        if (
            !espnTeamIdsByName
                .isEmpty()
        ) {
            return;
        }

        String rawJson =
            restClient
                .get()
                .uri(
                    "/apis/site/v2/sports/basketball/nba/teams?limit=100"
                )
                .accept(
                    MediaType.APPLICATION_JSON
                )
                .retrieve()
                .body(String.class);

        try {

            JsonNode root =
                objectMapper.readTree(
                    rawJson
                );

            Map<String, String>
                teamIds =
                    new HashMap<>();

            JsonNode sports =
                root.get(
                    "sports"
                );

            if (
                sports == null ||
                !sports.isArray()
            ) {
                return;
            }

            for (
                JsonNode sport :
                sports
            ) {

                JsonNode leagues =
                    sport.get(
                        "leagues"
                    );

                if (
                    leagues == null ||
                    !leagues.isArray()
                ) {
                    continue;
                }

                for (
                    JsonNode league :
                    leagues
                ) {

                    JsonNode teams =
                        league.get(
                            "teams"
                        );

                    if (
                        teams == null ||
                        !teams.isArray()
                    ) {
                        continue;
                    }

                    for (
                        JsonNode teamWrapper :
                        teams
                    ) {

                        JsonNode team =
                            teamWrapper.get(
                                "team"
                            );

                        if (
                            team == null
                        ) {
                            continue;
                        }

                        String teamId =
                            getText(
                                team,
                                "id"
                            );

                        String displayName =
                            getText(
                                team,
                                "displayName"
                            );

                        if (
                            teamId.isBlank() ||
                            displayName.isBlank()
                        ) {
                            continue;
                        }

                        teamIds.put(
                            normalizeName(
                                displayName
                            ),
                            teamId
                        );
                    }
                }
            }

            espnTeamIdsByName =
                Map.copyOf(
                    teamIds
                );

        } catch (Exception exception) {

            throw new RuntimeException(
                "Could not parse ESPN NBA teams",
                exception
            );
        }
    }

    private Map<String, String>
        loadTeamRoster(
            String teamName
        ) {

        String teamId =
            espnTeamIdsByName.get(
                normalizeName(
                    teamName
                )
            );

        if (
            teamId == null ||
            teamId.isBlank()
        ) {
            return Map.of();
        }

        try {

            String rawJson =
                restClient
                    .get()
                    .uri(
                        "/apis/site/v2/sports/basketball/nba/teams/"
                            + teamId
                            + "/roster"
                    )
                    .accept(
                        MediaType.APPLICATION_JSON
                    )
                    .retrieve()
                    .body(String.class);

            JsonNode root =
                objectMapper.readTree(
                    rawJson
                );

            JsonNode athletes =
                root.get(
                    "athletes"
                );

            if (
                athletes == null ||
                !athletes.isArray()
            ) {
                return Map.of();
            }

            Map<String, String>
                headshots =
                    new HashMap<>();

            for (
                JsonNode group :
                athletes
            ) {

                JsonNode items =
                    group.get(
                        "items"
                    );

                if (
                    items == null ||
                    !items.isArray()
                ) {
                    continue;
                }

                for (
                    JsonNode athlete :
                    items
                ) {

                    String playerName =
                        getText(
                            athlete,
                            "displayName"
                        );

                    JsonNode headshot =
                        athlete.get(
                            "headshot"
                        );

                    if (
                        playerName.isBlank() ||
                        headshot == null ||
                        headshot.isNull()
                    ) {
                        continue;
                    }

                    String imageUrl =
                        getText(
                            headshot,
                            "href"
                        );

                    if (
                        imageUrl.isBlank()
                    ) {
                        continue;
                    }

                    headshots.put(
                        normalizeName(
                            playerName
                        ),
                        imageUrl
                    );
                }
            }

            return Map.copyOf(
                headshots
            );

        } catch (Exception exception) {

            System.err.println(
                "Could not load ESPN NBA roster for "
                    + teamName
                    + ": "
                    + exception.getMessage()
            );

            return Map.of();
        }
    }

    private String getText(
        JsonNode node,
        String field
    ) {

        JsonNode value =
            node.get(
                field
            );

        if (
            value == null ||
            value.isNull()
        ) {
            return "";
        }

        return value.asString();
    }

    private String normalizeName(
        String value
    ) {

        if (
            value == null
        ) {
            return "";
        }

        String normalized =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFD
            )
            .replaceAll(
                "\\p{M}",
                ""
            )
            .toLowerCase(
                Locale.ROOT
            )
            .replaceAll(
                "[^a-z0-9]",
                ""
            );

        return normalized;
    }
}