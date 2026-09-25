package sportsalerts;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
@Order(2)
public class NbaTeamSyncService
        implements CommandLineRunner {

    private final TeamRepository teamRepository;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    private final String apiKey;

    public NbaTeamSyncService(
            TeamRepository teamRepository,
            ObjectMapper objectMapper,
            @Value("${SPORTRADAR_API_KEY:}") String apiKey) {

        this.teamRepository = teamRepository;

        this.objectMapper = objectMapper;

        this.apiKey = apiKey;

        this.restClient = RestClient.create(
                "https://api.sportradar.com");
    }

    @Override
    public void run(
            String... args) {

        syncNbaTeamIds();
    }

    public void syncNbaTeamIds() {

        if (apiKey == null ||
                apiKey.isBlank()) {

            System.out.println(
                    "Sportradar API key not configured. "
                            + "Skipping NBA team sync.");

            return;
        }

        List<Team> nbaTeams = teamRepository
                .findAll()
                .stream()
                .filter(
                        team -> "NBA".equals(
                                team.getLeague()))
                .toList();

        boolean needsSync = nbaTeams
                .stream()
                .anyMatch(
                        team -> team.getExternalProviderId() == null
                                ||
                                team.getExternalProviderId()
                                        .isBlank());

        if (!needsSync) {

            System.out.println(
                    "NBA Sportradar team IDs already synced.");

            return;
        }

        try {

            String response = restClient
                    .get()
                    .uri(
                            "/nba/trial/v8/en/league/teams.json")
                    .header(
                            "x-api-key",
                            apiKey)
                    .accept(
                            MediaType.APPLICATION_JSON)
                    .retrieve()
                    .body(
                            String.class);

            JsonNode root = objectMapper.readTree(
                    response);

            JsonNode teams = root.get(
                    "teams");

            if (teams == null ||
                    !teams.isArray()) {

                System.err.println(
                        "Sportradar NBA response "
                                + "did not contain a teams array.");

                return;
            }

            int syncedCount = 0;

            for (JsonNode providerTeam : teams) {

                String providerId = getText(
                        providerTeam,
                        "id");

                String market = getText(
                        providerTeam,
                        "market");

                String name = getText(
                        providerTeam,
                        "name");

                String fullName = (market + " " + name)
                        .trim();

                if (providerId.isBlank() ||
                        fullName.isBlank()) {

                    continue;
                }

                Team team = teamRepository
                        .findByLeagueAndName(
                                "NBA",
                                fullName)
                        .orElse(null);

                if (team == null) {
                    continue;
                }

                team.setExternalProviderId(
                        providerId);

                teamRepository.save(
                        team);

                syncedCount++;
            }

            System.out.println(
                    "NBA Sportradar team sync complete. "
                            + "Teams synced: "
                            + syncedCount);

        } catch (Exception exception) {

            System.err.println(
                    "NBA Sportradar team sync failed: "
                            + exception.getMessage());
        }
    }

    private String getText(
            JsonNode node,
            String fieldName) {

        JsonNode value = node.get(
                fieldName);

        if (value == null ||
                value.isNull()) {

            return "";
        }

        return value
                .asString()
                .trim();
    }
}