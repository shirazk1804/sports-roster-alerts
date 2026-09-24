package sportsalerts;

import java.text.Normalizer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class NflPlayerImageService {

    private record PlayerImageIds(
        String sleeperId,
        String espnId
    ) {
    }

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    /*
     * We keep two lightweight lookup maps:
     *
     * Sportradar UUID -> Sleeper/ESPN IDs
     * normalized player name -> Sleeper/ESPN IDs
     *
     * The name map is only a fallback when
     * Sleeper is missing a Sportradar ID.
     */
    private volatile Map<String, PlayerImageIds>
        playersBySportradarId =
            Map.of();

    private volatile Map<String, PlayerImageIds>
        playersByName =
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
     * Sleeper recommends downloading the
     * full NFL player map no more than about
     * once per day.
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

            Map<String, PlayerImageIds>
                refreshedBySportradar =
                    new HashMap<>();

            Map<String, PlayerImageIds>
                refreshedByName =
                    new HashMap<>();

            /*
             * Do not use a name fallback if
             * multiple NFL players normalize
             * to the same name.
             */
            Set<String> ambiguousNames =
                new HashSet<>();

            for (
                JsonNode player :
                root
            ) {

                String sleeperId =
                    getText(
                        player,
                        "player_id"
                    );

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

                String playerName =
                    getPlayerName(
                        player
                    );

                /*
                 * Without either an ESPN ID
                 * or Sleeper ID there is no
                 * useful image source.
                 */
                if (
                    sleeperId.isBlank() &&
                    espnId.isBlank()
                ) {
                    continue;
                }

                PlayerImageIds imageIds =
                    new PlayerImageIds(
                        sleeperId,
                        espnId
                    );

                /*
                 * Preferred lookup:
                 * Sportradar UUID.
                 */
                if (
                    !sportradarId.isBlank()
                ) {
                    refreshedBySportradar
                        .put(
                            sportradarId,
                            imageIds
                        );
                }

                /*
                 * Fallback lookup:
                 * normalized player name.
                 */
                String normalizedName =
                    normalizeName(
                        playerName
                    );

                if (
                    normalizedName.isBlank()
                ) {
                    continue;
                }

                if (
                    refreshedByName
                        .containsKey(
                            normalizedName
                        )
                ) {
                    ambiguousNames.add(
                        normalizedName
                    );

                } else {
                    refreshedByName.put(
                        normalizedName,
                        imageIds
                    );
                }
            }

            /*
             * Remove ambiguous names so we
             * never accidentally display
             * another player's photo.
             */
            for (
                String ambiguousName :
                ambiguousNames
            ) {
                refreshedByName.remove(
                    ambiguousName
                );
            }

            playersBySportradarId =
                Map.copyOf(
                    refreshedBySportradar
                );

            playersByName =
                Map.copyOf(
                    refreshedByName
                );

            System.out.println(
                "Sleeper NFL player image map refreshed. "
                    + "Sportradar matches: "
                    + refreshedBySportradar.size()
                    + ". Unique name matches: "
                    + refreshedByName.size()
                    + "."
            );

        } catch (Exception exception) {

            /*
             * Keep the previous successful
             * cache if Sleeper temporarily
             * fails.
             */
            System.err.println(
                "Could not refresh Sleeper NFL "
                    + "player image map: "
                    + exception.getMessage()
            );
        }
    }

    /*
     * Keep this overload so existing code
     * continues to compile.
     */
    public String getHeadshotUrl(
        String sportradarPlayerId
    ) {
        return getHeadshotUrl(
            sportradarPlayerId,
            null
        );
    }

    /*
     * Preferred image:
     *
     * Sleeper's player CDN.
     *
     * If the Sportradar cross-reference is
     * missing, we fall back to an exact,
     * normalized player-name match.
     */
    public String getHeadshotUrl(
        String sportradarPlayerId,
        String playerName
    ) {

        PlayerImageIds player =
            findPlayer(
                sportradarPlayerId,
                playerName
            );

        if (player == null) {
            return null;
        }

        if (
            player.sleeperId() != null &&
            !player.sleeperId()
                .isBlank()
        ) {
            return (
                "https://sleepercdn.com/"
                    + "content/nfl/players/"
                    + player.sleeperId()
                    + ".jpg"
            );
        }

        if (
            player.espnId() != null &&
            !player.espnId()
                .isBlank()
        ) {
            return buildEspnUrl(
                player.espnId()
            );
        }

        return null;
    }

    /*
     * Secondary URL used if the Sleeper
     * image fails to load.
     */
    public String getFallbackHeadshotUrl(
        String sportradarPlayerId,
        String playerName
    ) {

        PlayerImageIds player =
            findPlayer(
                sportradarPlayerId,
                playerName
            );

        if (
            player == null ||
            player.espnId() == null ||
            player.espnId()
                .isBlank()
        ) {
            return null;
        }

        return buildEspnUrl(
            player.espnId()
        );
    }

    private PlayerImageIds findPlayer(
        String sportradarPlayerId,
        String playerName
    ) {

        /*
         * Always prefer the provider ID.
         */
        if (
            sportradarPlayerId != null &&
            !sportradarPlayerId.isBlank()
        ) {

            PlayerImageIds byId =
                playersBySportradarId.get(
                    sportradarPlayerId
                );

            if (byId != null) {
                return byId;
            }
        }

        /*
         * Fall back to player name only if
         * the ID match was unavailable.
         */
        String normalizedName =
            normalizeName(
                playerName
            );

        if (normalizedName.isBlank()) {
            return null;
        }

        return playersByName.get(
            normalizedName
        );
    }

    private String buildEspnUrl(
        String espnId
    ) {
        return (
            "https://a.espncdn.com/i/headshots/"
                + "nfl/players/full/"
                + espnId
                + ".png"
        );
    }

    private String getPlayerName(
        JsonNode player
    ) {

        String fullName =
            getText(
                player,
                "full_name"
            );

        if (!fullName.isBlank()) {
            return fullName;
        }

        String firstName =
            getText(
                player,
                "first_name"
            );

        String lastName =
            getText(
                player,
                "last_name"
            );

        return (
            firstName
                + " "
                + lastName
        ).trim();
    }

    private String normalizeName(
        String value
    ) {

        if (
            value == null ||
            value.isBlank()
        ) {
            return "";
        }

        String normalized =
            Normalizer.normalize(
                value,
                Normalizer.Form.NFD
            )
            .replaceAll(
                "\\p{M}+",
                ""
            )
            .toLowerCase()
            .replaceAll(
                "[^a-z0-9 ]",
                " "
            )
            .trim()
            .replaceAll(
                "\\s+",
                " "
            );

        if (normalized.isBlank()) {
            return "";
        }

        /*
         * Sportradar and Sleeper may disagree
         * on suffix formatting such as:
         *
         * Jr.
         * Sr.
         * II
         * III
         */
        String[] pieces =
            normalized.split(" ");

        if (pieces.length > 1) {

            String lastPiece =
                pieces[
                    pieces.length - 1
                ];

            if (
                lastPiece.equals("jr") ||
                lastPiece.equals("sr") ||
                lastPiece.equals("ii") ||
                lastPiece.equals("iii") ||
                lastPiece.equals("iv") ||
                lastPiece.equals("v")
            ) {

                StringBuilder withoutSuffix =
                    new StringBuilder();

                for (
                    int index = 0;
                    index <
                        pieces.length - 1;
                    index++
                ) {

                    if (
                        withoutSuffix.length()
                            > 0
                    ) {
                        withoutSuffix.append(
                            " "
                        );
                    }

                    withoutSuffix.append(
                        pieces[index]
                    );
                }

                normalized =
                    withoutSuffix
                        .toString();
            }
        }

        return normalized
            .replace(
                " ",
                ""
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