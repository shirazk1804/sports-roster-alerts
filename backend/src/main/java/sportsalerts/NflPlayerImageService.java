package sportsalerts;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class NflPlayerImageService {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    /*
     * Only this small mapping stays in memory:
     *
     * Sportradar player UUID -> ESPN player ID
     */
    private volatile Map<String, String>
        espnIdsBySportradarId =
            Map.of();

    public NflPlayerImageService(
        ObjectMapper objectMapper
    ) {
        this.objectMapper =
            objectMapper;

        this.restClient =
            RestClient.create(
                "https://api.sleeper.app"
            );
    }

    /*
     * Refresh shortly after backend startup,
     * then once every 24 hours.
     *
     * Sleeper specifically recommends that
     * the full NFL player map not be fetched
     * more than about once per day.
     */
    @Scheduled(
        initialDelay = 10000,
        fixedDelay = 86400000
    )
    public void refreshPlayerIds() {

        try {
            String rawJson =
                restClient
                    .get()
                    .uri(
                        "/v1/players/nfl"
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

            Map<String, String>
                refreshedMap =
                    new HashMap<>();

            /*
             * Sleeper returns one large
             * JSON object where each value
             * represents an NFL player.
             */
            for (
                JsonNode player :
                root
            ) {

                String sportradarId =
                    getText(
                        player,
                        "sportradar_id"
                    );

                String espnId =
                    getText(
                        player,
                        "espn_id"
                    );

                if (
                    sportradarId.isBlank() ||
                    espnId.isBlank()
                ) {
                    continue;
                }

                refreshedMap.put(
                    sportradarId,
                    espnId
                );
            }

            /*
             * Replace the entire map at once.
             * Requests can safely keep reading
             * the previous map while refresh
             * work is happening.
             */
            espnIdsBySportradarId =
                Map.copyOf(
                    refreshedMap
                );

            System.out.println(
                "Sleeper NFL player ID map refreshed. "
                    + "Matched "
                    + refreshedMap.size()
                    + " Sportradar/ESPN IDs."
            );

        } catch (Exception exception) {

            /*
             * Keep the previous successful
             * mapping if Sleeper temporarily
             * fails.
             */
            System.err.println(
                "Could not refresh Sleeper NFL "
                    + "player IDs: "
                    + exception.getMessage()
            );
        }
    }

    public String getHeadshotUrl(
        String sportradarPlayerId
    ) {

        if (
            sportradarPlayerId == null ||
            sportradarPlayerId.isBlank()
        ) {
            return null;
        }

        String espnId =
            espnIdsBySportradarId.get(
                sportradarPlayerId
            );

        if (
            espnId == null ||
            espnId.isBlank()
        ) {
            return null;
        }

        return (
            "https://a.espncdn.com/i/headshots/"
                + "nfl/players/full/"
                + espnId
                + ".png"
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
}