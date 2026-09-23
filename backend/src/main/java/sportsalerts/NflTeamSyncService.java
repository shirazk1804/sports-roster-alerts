package sportsalerts;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class NflTeamSyncService
    implements CommandLineRunner {

    private final TeamRepository teamRepository;

    private final ObjectMapper objectMapper;

    private final RestClient restClient;

    private final String apiKey;

    public NflTeamSyncService(
        TeamRepository teamRepository,
        ObjectMapper objectMapper,
        @Value("${SPORTRADAR_API_KEY:}")
        String apiKey
    ) {
        this.teamRepository =
            teamRepository;

        this.objectMapper =
            objectMapper;

        this.apiKey =
            apiKey;

        this.restClient =
            RestClient.create(
                "https://api.sportradar.com"
            );
    }

    @Override
    public void run(
        String... args
    ) {
        syncNflTeamIds();
    }

    public void syncNflTeamIds() {

        if (
            apiKey == null ||
            apiKey.isBlank()
        ) {
            System.out.println(
                "Sportradar API key not configured. "
                + "Skipping NFL team sync."
            );

            return;
        }

        List<Team> nflTeams =
            teamRepository
                .findAll()
                .stream()
                .filter(
                    team ->
                        "NFL".equals(
                            team.getLeague()
                        )
                )
                .toList();

        boolean needsSync =
            nflTeams
                .stream()
                .anyMatch(
                    team ->
                        team.getExternalProviderId()
                            == null ||
                        team.getExternalProviderId()
                            .isBlank()
                );

        if (!needsSync) {
            System.out.println(
                "NFL Sportradar team IDs already synced."
            );

            return;
        }

        try {
            String response =
                restClient
                    .get()
                    .uri(
                        "/nfl/official/trial/v7/en/league/teams.json"
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

            JsonNode root =
                objectMapper.readTree(
                    response
                );

            JsonNode teams =
                root.get("teams");

            if (
                teams == null ||
                !teams.isArray()
            ) {
                System.err.println(
                    "Sportradar NFL response "
                    + "did not contain a teams array."
                );

                return;
            }

            int syncedCount = 0;

            for (
                JsonNode providerTeam :
                teams
            ) {
                String providerId =
                    getText(
                        providerTeam,
                        "id"
                    );

                String market =
                    getText(
                        providerTeam,
                        "market"
                    );

                String name =
                    getText(
                        providerTeam,
                        "name"
                    );

                String fullName =
                    (market + " " + name)
                        .trim();

                if (
                    providerId.isBlank() ||
                    fullName.isBlank()
                ) {
                    continue;
                }

                Team team =
                    teamRepository
                        .findByLeagueAndName(
                            "NFL",
                            fullName
                        )
                        .orElse(null);

                if (team == null) {
                    System.err.println(
                        "Could not match Sportradar NFL team: "
                        + fullName
                    );

                    continue;
                }

                team.setExternalProviderId(
                    providerId
                );

                teamRepository.save(
                    team
                );

                syncedCount++;
            }

            System.out.println(
                "NFL Sportradar team sync complete. "
                + "Teams synced: "
                + syncedCount
            );

        } catch (Exception exception) {

            System.err.println(
                "NFL Sportradar team sync failed: "
                + exception.getMessage()
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