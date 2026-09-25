package sportsalerts;

import java.time.Instant;
import java.time.OffsetDateTime;
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

        private final SportradarRequestLimiter requestLimiter;

        public NflInjuryService(
                        ObjectMapper objectMapper,
                        @Value("${SPORTRADAR_API_KEY:}") String apiKey,
                        SportradarRequestLimiter requestLimiter) {
                this.objectMapper = objectMapper;

                this.apiKey = apiKey;

                this.requestLimiter = requestLimiter;

                this.restClient = RestClient.create(
                                "https://api.sportradar.com");
        }

        /*
         * Do NOT use Sportradar's current_week
         * endpoint here.
         *
         * That endpoint can remain on the previous
         * NFL week until a game in the next week
         * reaches "created" status.
         *
         * Instead, load the full current-season
         * schedule and find the week containing
         * the next scheduled NFL game.
         */
        public NflWeekInfo getUpcomingWeek() {

                ensureApiKey();

                try {
                        /*
                         * Step 1:
                         *
                         * Use current_week ONLY to determine
                         * the active season year and type.
                         *
                         * We intentionally ignore the week
                         * number because Sportradar can keep
                         * current_week on the previous week.
                         */
                        String currentWeekResponse = requestLimiter.execute(
                                        () -> restClient
                                                        .get()
                                                        .uri(
                                                                        "/nfl/official/trial/v7/en/games/current_week/schedule.json")
                                                        .header(
                                                                        "x-api-key",
                                                                        apiKey)
                                                        .accept(
                                                                        MediaType.APPLICATION_JSON)
                                                        .retrieve()
                                                        .body(String.class));

                        JsonNode currentWeekRoot = objectMapper.readTree(
                                        currentWeekResponse);

                        JsonNode yearNode = currentWeekRoot.get(
                                        "year");

                        JsonNode typeNode = currentWeekRoot.get(
                                        "type");

                        if (yearNode == null ||
                                        typeNode == null) {
                                throw new IllegalStateException(
                                                "Current week response did not contain year/type");
                        }

                        int seasonYear = yearNode.asInt();

                        String seasonType = typeNode.asString();

                        /*
                         * Step 2:
                         *
                         * Fetch the explicit full season
                         * schedule.
                         *
                         * Unlike current_week, we are not
                         * trusting Sportradar to choose which
                         * week is current.
                         */
                        String seasonScheduleResponse = requestLimiter.execute(
                                        () -> restClient
                                                        .get()
                                                        .uri(
                                                                        "/nfl/official/trial/v7/en/games/"
                                                                                        + seasonYear
                                                                                        + "/"
                                                                                        + seasonType
                                                                                        + "/schedule.json")
                                                        .header(
                                                                        "x-api-key",
                                                                        apiKey)
                                                        .accept(
                                                                        MediaType.APPLICATION_JSON)
                                                        .retrieve()
                                                        .body(String.class));

                        JsonNode seasonRoot = objectMapper.readTree(
                                        seasonScheduleResponse);

                        JsonNode weeks = seasonRoot.get(
                                        "weeks");

                        if (weeks == null ||
                                        !weeks.isArray()) {
                                throw new IllegalStateException(
                                                "Season schedule did not contain weeks");
                        }

                        Instant now = Instant.now();

                        Instant earliestFutureGame = null;

                        Integer upcomingWeek = null;

                        /*
                         * Step 3:
                         *
                         * Find the earliest NFL game whose
                         * scheduled kickoff is still in the
                         * future.
                         *
                         * Whichever week contains that game
                         * is the actual upcoming week.
                         */
                        for (JsonNode week : weeks) {
                                JsonNode sequenceNode = week.get(
                                                "sequence");

                                JsonNode games = week.get(
                                                "games");

                                if (sequenceNode == null ||
                                                games == null ||
                                                !games.isArray()) {
                                        continue;
                                }

                                int weekNumber = sequenceNode.asInt();

                                for (JsonNode game : games) {
                                        String scheduled = getText(
                                                        game,
                                                        "scheduled");

                                        if (scheduled.isBlank()) {
                                                continue;
                                        }

                                        String status = getText(
                                                        game,
                                                        "status");

                                        if ("cancelled".equalsIgnoreCase(
                                                        status) ||
                                                        "canceled".equalsIgnoreCase(
                                                                        status)) {
                                                continue;
                                        }

                                        Instant gameTime;

                                        try {
                                                gameTime = Instant.parse(
                                                                scheduled);

                                        } catch (Exception exception) {

                                                try {
                                                        gameTime = OffsetDateTime
                                                                        .parse(
                                                                                        scheduled)
                                                                        .toInstant();

                                                } catch (Exception ignored) {
                                                        continue;
                                                }
                                        }

                                        if (!gameTime.isAfter(
                                                        now)) {
                                                continue;
                                        }

                                        if (earliestFutureGame == null ||
                                                        gameTime.isBefore(
                                                                        earliestFutureGame)) {
                                                earliestFutureGame = gameTime;

                                                upcomingWeek = weekNumber;
                                        }
                                }
                        }

                        if (upcomingWeek == null) {
                                throw new IllegalStateException(
                                                "No future game found in "
                                                                + seasonYear
                                                                + " "
                                                                + seasonType
                                                                + " season schedule");
                        }

                        System.out.println(
                                        "Upcoming NFL week resolved: "
                                                        + seasonYear
                                                        + " "
                                                        + seasonType
                                                        + " Week "
                                                        + upcomingWeek);

                        return new NflWeekInfo(
                                        seasonYear,
                                        seasonType,
                                        upcomingWeek);

                } catch (Exception exception) {

                        throw new RuntimeException(
                                        "Could not determine upcoming NFL week: "
                                                        + exception.getMessage(),
                                        exception);
                }
        }

        public String getWeeklyInjuries(
                        NflWeekInfo week) {

                ensureApiKey();

                return requestLimiter.execute(
                                () -> restClient
                                                .get()
                                                .uri(
                                                                "/nfl/official/trial/v7/en/seasons/"
                                                                                + week.seasonYear()
                                                                                + "/"
                                                                                + week.seasonType()
                                                                                + "/"
                                                                                + week.week()
                                                                                + "/injuries.json")
                                                .header(
                                                                "x-api-key",
                                                                apiKey)
                                                .accept(
                                                                MediaType.APPLICATION_JSON)
                                                .retrieve()
                                                .body(String.class));
        }

        public List<NflInjuryEvent> getNormalizedInjuriesForTeam(
                        String rawJson,
                        String externalProviderTeamId) {

                List<NflInjuryEvent> events = new ArrayList<>();

                try {
                        JsonNode root = objectMapper.readTree(
                                        rawJson);

                        JsonNode teams = root.get("teams");

                        if (teams == null ||
                                        !teams.isArray()) {
                                return events;
                        }

                        for (JsonNode team : teams) {
                                String teamId = getText(
                                                team,
                                                "id");

                                if (!externalProviderTeamId
                                                .equals(
                                                                teamId)) {
                                        continue;
                                }

                                String market = getText(
                                                team,
                                                "market");

                                String name = getText(
                                                team,
                                                "name");

                                String teamName = (market
                                                + " "
                                                + name).trim();

                                JsonNode players = team.get(
                                                "players");

                                if (players == null ||
                                                !players.isArray()) {
                                        continue;
                                }

                                for (JsonNode player : players) {
                                        String playerId = getText(
                                                        player,
                                                        "id");

                                        String playerName = getText(
                                                        player,
                                                        "name");

                                        String position = getText(
                                                        player,
                                                        "position");

                                        JsonNode injuries = player.get(
                                                        "injuries");

                                        if (injuries == null ||
                                                        !injuries.isArray()) {
                                                continue;
                                        }

                                        for (JsonNode injury : injuries) {
                                                String primary = getText(
                                                                injury,
                                                                "primary");

                                                String secondary = getText(
                                                                injury,
                                                                "secondary");

                                                String gameStatus = getText(
                                                                injury,
                                                                "status");

                                                String statusDate = getText(
                                                                injury,
                                                                "status_date");

                                                String estimatedReturnDate = getText(
                                                                injury,
                                                                "estimated_return_date");

                                                String practiceStatus = "";

                                                JsonNode practice = injury.get(
                                                                "practice");

                                                if (practice != null &&
                                                                !practice.isNull()) {
                                                        practiceStatus = getText(
                                                                        practice,
                                                                        "status");
                                                }

                                                String description = buildDescription(
                                                                playerName,
                                                                primary,
                                                                secondary,
                                                                gameStatus,
                                                                practiceStatus);

                                                events.add(
                                                                new NflInjuryEvent(
                                                                                teamId,
                                                                                teamName,
                                                                                playerId,
                                                                                playerName,
                                                                                position,
                                                                                primary,
                                                                                secondary,
                                                                                gameStatus,
                                                                                practiceStatus,
                                                                                statusDate,
                                                                                estimatedReturnDate,
                                                                                description));
                                        }
                                }
                        }

                        return events;

                } catch (Exception exception) {

                        throw new RuntimeException(
                                        "Could not parse NFL injuries",
                                        exception);
                }
        }

        public String getRawTeamInjuryBlock(
                        String rawJson,
                        String externalProviderTeamId) {

                try {
                        JsonNode root = objectMapper.readTree(
                                        rawJson);

                        JsonNode teams = root.get("teams");

                        if (teams == null ||
                                        !teams.isArray()) {
                                return "No teams array";
                        }

                        for (JsonNode team : teams) {

                                String teamId = getText(
                                                team,
                                                "id");

                                if (externalProviderTeamId
                                                .equals(teamId)) {

                                        return team
                                                        .toPrettyString();
                                }
                        }

                        return "Team not found in injury response";

                } catch (Exception exception) {

                        return "Could not inspect team injury JSON: "
                                        + exception.getMessage();
                }
        }

        private String buildDescription(
                        String playerName,
                        String primary,
                        String secondary,
                        String gameStatus,
                        String practiceStatus) {
                StringBuilder description = new StringBuilder();

                description.append(
                                playerName);

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

                JsonNode value = node.get(
                                field);

                if (value == null ||
                                value.isNull()) {
                        return "";
                }

                return value.asString();
        }
}