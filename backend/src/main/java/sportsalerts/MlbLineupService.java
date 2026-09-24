package sportsalerts;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class MlbLineupService {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    public MlbLineupService(
            ObjectMapper objectMapper) {

        this.objectMapper =
                objectMapper;

        this.restClient =
                RestClient.create(
                        "https://statsapi.mlb.com");
    }

    public MlbLineupResponse getLineup(
            Long teamId,
            LocalDate date) {

        try {
            String scheduleJson =
                    getSchedule(
                            teamId,
                            date);

            JsonNode scheduleRoot =
                    objectMapper.readTree(
                            scheduleJson);

            JsonNode game =
                    findGame(
                            scheduleRoot);

            if (game == null) {
                return new MlbLineupResponse(
                        "NO_GAME",
                        null,
                        null,
                        null,
                        null,
                        null,
                        List.of(),
                        null);
            }

            Long gamePk =
                    getLong(
                            game,
                            "gamePk");

            String gameDate =
                    getText(
                            game,
                            "gameDate");

            JsonNode status =
                    game.get(
                            "status");

            String gameStatus =
                    status == null
                            ? ""
                            : getText(
                                    status,
                                    "detailedState");

            String teamSide =
                    getTeamSide(
                            game,
                            teamId);

            if (teamSide == null) {
                throw new RuntimeException(
                        "Could not determine MLB team side");
            }

            String opponentSide =
                    teamSide.equals("home")
                            ? "away"
                            : "home";

            JsonNode gameTeams =
                    game.get(
                            "teams");

            JsonNode opponent =
                    gameTeams
                            .get(opponentSide)
                            .get("team");

            String opponentName =
                    getText(
                            opponent,
                            "name");

            String homeAway =
                    teamSide.equals("home")
                            ? "HOME"
                            : "AWAY";

            JsonNode probablePitcher =
                    gameTeams
                            .get(teamSide)
                            .get(
                                    "probablePitcher");

            /*
             * The schedule exists before a
             * lineup is posted, so failure to
             * load the boxscore should simply
             * mean the lineup is not available
             * yet.
             */
            JsonNode teamBoxscore = null;

            if (gamePk != null) {
                try {
                    String boxscoreJson =
                            getBoxscore(
                                    gamePk);

                    JsonNode boxscoreRoot =
                            objectMapper.readTree(
                                    boxscoreJson);

                    JsonNode boxscoreTeams =
                            boxscoreRoot.get(
                                    "teams");

                    if (boxscoreTeams != null) {
                        teamBoxscore =
                                boxscoreTeams.get(
                                        teamSide);
                    }

                } catch (Exception exception) {
                    System.err.println(
                            "Could not load MLB boxscore "
                                    + "for game "
                                    + gamePk
                                    + ": "
                                    + exception.getMessage());
                }
            }

            List<MlbLineupPlayerResponse>
                    lineup =
                    getStartingLineup(
                            teamBoxscore);

            MlbLineupPlayerResponse
                    startingPitcher =
                    getStartingPitcher(
                            teamBoxscore,
                            probablePitcher);

            String state =
                    lineup.size() >= 9
                            ? "POSTED"
                            : "NOT_POSTED";

            return new MlbLineupResponse(
                    state,
                    gamePk,
                    gameDate,
                    gameStatus,
                    opponentName,
                    homeAway,
                    lineup,
                    startingPitcher);

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Could not load MLB lineup",
                    exception);
        }
    }

    private String getSchedule(
            Long teamId,
            LocalDate date) {

        return restClient
                .get()
                .uri(
                        uriBuilder ->
                                uriBuilder
                                        .path(
                                                "/api/v1/schedule")
                                        .queryParam(
                                                "sportId",
                                                1)
                                        .queryParam(
                                                "teamId",
                                                teamId)
                                        .queryParam(
                                                "date",
                                                date.toString())
                                        .queryParam(
                                                "hydrate",
                                                "probablePitcher")
                                        .build())
                .retrieve()
                .body(
                        String.class);
    }

    private String getBoxscore(
            Long gamePk) {

        return restClient
                .get()
                .uri(
                        "/api/v1/game/"
                                + gamePk
                                + "/boxscore")
                .retrieve()
                .body(
                        String.class);
    }

    /*
     * Normally there is one game per team
     * per day.
     *
     * For a doubleheader, prefer the first
     * game that has not finished yet.
     * If every game is final, return the
     * last game of the day.
     */
    private JsonNode findGame(
            JsonNode root) {

        if (root == null) {
            return null;
        }

        JsonNode dates =
                root.get(
                        "dates");

        if (dates == null ||
                !dates.isArray() ||
                dates.size() == 0) {

            return null;
        }

        JsonNode games =
                dates
                        .get(0)
                        .get(
                                "games");

        if (games == null ||
                !games.isArray() ||
                games.size() == 0) {

            return null;
        }

        JsonNode lastGame = null;

        for (JsonNode game : games) {

            lastGame = game;

            JsonNode status =
                    game.get(
                            "status");

            String abstractState =
                    status == null
                            ? ""
                            : getText(
                                    status,
                                    "abstractGameState");

            if (!"Final".equalsIgnoreCase(
                    abstractState)) {

                return game;
            }
        }

        return lastGame;
    }

    private String getTeamSide(
            JsonNode game,
            Long teamId) {

        JsonNode teams =
                game.get(
                        "teams");

        if (teams == null) {
            return null;
        }

        JsonNode home =
                teams
                        .get("home")
                        .get("team");

        Long homeTeamId =
                getLong(
                        home,
                        "id");

        if (teamId.equals(
                homeTeamId)) {

            return "home";
        }

        JsonNode away =
                teams
                        .get("away")
                        .get("team");

        Long awayTeamId =
                getLong(
                        away,
                        "id");

        if (teamId.equals(
                awayTeamId)) {

            return "away";
        }

        return null;
    }

    private List<MlbLineupPlayerResponse>
            getStartingLineup(
                    JsonNode teamBoxscore) {

        List<MlbLineupPlayerResponse>
                lineup =
                new ArrayList<>();

        if (teamBoxscore == null) {
            return lineup;
        }

        JsonNode battingOrder =
                teamBoxscore.get(
                        "battingOrder");

        JsonNode players =
                teamBoxscore.get(
                        "players");

        if (battingOrder == null ||
                !battingOrder.isArray() ||
                players == null) {

            return lineup;
        }

        Set<Long> addedPlayers =
                new HashSet<>();

        int fallbackOrder = 1;

        for (JsonNode playerIdNode :
                battingOrder) {

            Long playerId =
                    playerIdNode.asLong();

            JsonNode player =
                    players.get(
                            "ID" + playerId);

            if (player == null) {
                continue;
            }

            String battingOrderCode =
                    getText(
                            player,
                            "battingOrder");

            /*
             * MLB batting-order codes generally
             * use values such as:
             *
             * 100 = original leadoff hitter
             * 200 = original #2 hitter
             * ...
             *
             * A replacement can use a value
             * such as 101.
             *
             * We only want the original
             * starting lineup.
             */
            if (!battingOrderCode.isBlank() &&
                    !battingOrderCode.endsWith(
                            "0")) {

                continue;
            }

            if (addedPlayers.contains(
                    playerId)) {

                continue;
            }

            Integer order =
                    parseBattingOrder(
                            battingOrderCode);

            if (order == null) {
                order =
                        fallbackOrder;
            }

            if (order < 1 ||
                    order > 9) {

                continue;
            }

            JsonNode person =
                    player.get(
                            "person");

            String playerName =
                    person == null
                            ? "Unknown Player"
                            : getText(
                                    person,
                                    "fullName");

            JsonNode position =
                    player.get(
                            "position");

            String positionName =
                    position == null
                            ? ""
                            : getText(
                                    position,
                                    "abbreviation");

            lineup.add(
                    new MlbLineupPlayerResponse(
                            playerId,
                            playerName,
                            positionName,
                            order));

            addedPlayers.add(
                    playerId);

            fallbackOrder++;
        }

        lineup.sort(
                Comparator.comparing(
                        MlbLineupPlayerResponse
                                ::battingOrder));

        return lineup;
    }

    private MlbLineupPlayerResponse
            getStartingPitcher(
                    JsonNode teamBoxscore,
                    JsonNode probablePitcher) {

        if (teamBoxscore != null) {

            JsonNode pitchers =
                    teamBoxscore.get(
                            "pitchers");

            JsonNode players =
                    teamBoxscore.get(
                            "players");

            if (pitchers != null &&
                    pitchers.isArray() &&
                    pitchers.size() > 0 &&
                    players != null) {

                Long pitcherId =
                        pitchers
                                .get(0)
                                .asLong();

                JsonNode pitcher =
                        players.get(
                                "ID"
                                        + pitcherId);

                if (pitcher != null) {

                    JsonNode person =
                            pitcher.get(
                                    "person");

                    String pitcherName =
                            person == null
                                    ? "Unknown Player"
                                    : getText(
                                            person,
                                            "fullName");

                    return new MlbLineupPlayerResponse(
                            pitcherId,
                            pitcherName,
                            "SP",
                            null);
                }
            }
        }

        /*
         * Before the official lineup appears,
         * the schedule often still contains
         * the probable starter.
         */
        if (probablePitcher != null &&
                !probablePitcher.isNull()) {

            Long pitcherId =
                    getLong(
                            probablePitcher,
                            "id");

            String pitcherName =
                    getText(
                            probablePitcher,
                            "fullName");

            if (pitcherId != null ||
                    !pitcherName.isBlank()) {

                return new MlbLineupPlayerResponse(
                        pitcherId,
                        pitcherName,
                        "SP",
                        null);
            }
        }

        return null;
    }

    private Integer parseBattingOrder(
            String value) {

        if (value == null ||
                value.isBlank()) {

            return null;
        }

        try {
            int rawValue =
                    Integer.parseInt(
                            value);

            if (rawValue >= 100) {
                return rawValue / 100;
            }

            return rawValue;

        } catch (
                NumberFormatException exception) {

            return null;
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

        return value.asString();
    }

    private Long getLong(
            JsonNode node,
            String field) {

        if (node == null) {
            return null;
        }

        JsonNode value =
                node.get(
                        field);

        if (value == null ||
                value.isNull()) {

            return null;
        }

        return value.asLong();
    }
}