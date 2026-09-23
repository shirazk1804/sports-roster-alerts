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
public class NflTransactionService {

    private final RestClient restClient;

    private final ObjectMapper objectMapper;

    private final String apiKey;

    public NflTransactionService(
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

    public String getDailyTransactions(
        LocalDate date
    ) {
        if (
            apiKey == null ||
            apiKey.isBlank()
        ) {
            throw new IllegalStateException(
                "Sportradar API key is not configured"
            );
        }

        String year =
            String.valueOf(
                date.getYear()
            );

        String month =
            String.format(
                "%02d",
                date.getMonthValue()
            );

        String day =
            String.format(
                "%02d",
                date.getDayOfMonth()
            );

        return restClient
            .get()
            .uri(
                "/nfl/official/trial/v7/en/league/"
                    + year
                    + "/"
                    + month
                    + "/"
                    + day
                    + "/transactions.json"
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

    public List<NflTransactionEvent>
        getNormalizedTransactionsForTeam(
            String rawJson,
            String externalProviderTeamId
        ) {

        List<NflTransactionEvent> events =
            new ArrayList<>();

        try {
            JsonNode root =
                objectMapper.readTree(
                    rawJson
                );

            JsonNode players =
                root.get("players");

            if (
                players == null ||
                !players.isArray()
            ) {
                return events;
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

                JsonNode transactions =
                    player.get(
                        "transactions"
                    );

                if (
                    transactions == null ||
                    !transactions.isArray()
                ) {
                    continue;
                }

                for (
                    JsonNode transaction :
                    transactions
                ) {
                    if (
                        !belongsToTeam(
                            transaction,
                            externalProviderTeamId
                        )
                    ) {
                        continue;
                    }

                    String transactionId =
                        getText(
                            transaction,
                            "id"
                        );

                    String description =
                        getText(
                            transaction,
                            "desc"
                        );

                    String effectiveDate =
                        getText(
                            transaction,
                            "effective_date"
                        );

                    String transactionCode =
                        getText(
                            transaction,
                            "transaction_code"
                        );

                    String transactionType =
                        getText(
                            transaction,
                            "transaction_type"
                        );

                    String statusBefore =
                        getText(
                            transaction,
                            "status_before"
                        );

                    String statusAfter =
                        getText(
                            transaction,
                            "status_after"
                        );

                    String eventType =
                        normalizeEventType(
                            transactionCode,
                            transactionType,
                            description,
                            statusBefore,
                            statusAfter
                        );

                    if (eventType == null) {
                        System.out.println(
                            "Unmapped NFL transaction: "
                                + transactionCode
                                + " | "
                                + transactionType
                                + " | "
                                + description
                        );

                        continue;
                    }

                    String teamName =
                        getTeamName(
                            transaction,
                            externalProviderTeamId
                        );

                    events.add(
                        new NflTransactionEvent(
                            transactionId,
                            playerId,
                            playerName,
                            externalProviderTeamId,
                            teamName,
                            eventType,
                            effectiveDate,
                            description,
                            transactionCode,
                            statusBefore,
                            statusAfter
                        )
                    );
                }
            }

            return events;

        } catch (Exception exception) {

            throw new RuntimeException(
                "Could not parse NFL transactions",
                exception
            );
        }
    }

    private boolean belongsToTeam(
        JsonNode transaction,
        String externalProviderTeamId
    ) {
        JsonNode fromTeam =
            transaction.get(
                "from_team"
            );

        if (
            teamMatches(
                fromTeam,
                externalProviderTeamId
            )
        ) {
            return true;
        }

        JsonNode toTeam =
            transaction.get(
                "to_team"
            );

        return teamMatches(
            toTeam,
            externalProviderTeamId
        );
    }

    private boolean teamMatches(
        JsonNode team,
        String externalProviderTeamId
    ) {
        if (
            team == null ||
            team.isNull()
        ) {
            return false;
        }

        String teamId =
            getText(
                team,
                "id"
            );

        return externalProviderTeamId
            .equals(teamId);
    }

    private String getTeamName(
        JsonNode transaction,
        String externalProviderTeamId
    ) {
        JsonNode toTeam =
            transaction.get(
                "to_team"
            );

        if (
            teamMatches(
                toTeam,
                externalProviderTeamId
            )
        ) {
            return buildTeamName(
                toTeam
            );
        }

        JsonNode fromTeam =
            transaction.get(
                "from_team"
            );

        if (
            teamMatches(
                fromTeam,
                externalProviderTeamId
            )
        ) {
            return buildTeamName(
                fromTeam
            );
        }

        return "Unknown Team";
    }

    private String buildTeamName(
        JsonNode team
    ) {
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

        return (
            market + " " + name
        ).trim();
    }

    private String normalizeEventType(
        String transactionCode,
        String transactionType,
        String description,
        String statusBefore,
        String statusAfter
    ) {
        String code =
            transactionCode == null
                ? ""
                : transactionCode
                    .toUpperCase();

        String type =
            transactionType == null
                ? ""
                : transactionType
                    .toLowerCase();

        String desc =
            description == null
                ? ""
                : description
                    .toLowerCase();

        String before =
            statusBefore == null
                ? ""
                : statusBefore
                    .toUpperCase();

        String after =
            statusAfter == null
                ? ""
                : statusAfter
                    .toUpperCase();

        /*
         * Injured Reserve
         */
        if (
            "IR".equals(after) &&
            !"IR".equals(before)
        ) {
            return "INJURED_RESERVE";
        }

        if (
            "IRD".equals(after) &&
            !"IRD".equals(before)
        ) {
            return "IR_DESIGNATED_RETURN";
        }

        /*
         * PUP / NFI-type roster statuses
         */
        if (
            "PUP".equals(after) &&
            !"PUP".equals(before)
        ) {
            return "PUP_PLACEMENT";
        }

        if (
            "NON".equals(after) &&
            !"NON".equals(before)
        ) {
            return "NFI_PLACEMENT";
        }

        /*
         * Practice Squad
         */
        if (
            "PRA".equals(after) &&
            !"PRA".equals(before)
        ) {
            return "PRACTICE_SQUAD";
        }

        /*
         * Suspension
         */
        if (
            "SUS".equals(after) &&
            !"SUS".equals(before)
        ) {
            return "SUSPENDED";
        }

        if (
            "SUS".equals(before) &&
            "ACT".equals(after)
        ) {
            return "SUSPENSION_REINSTATED";
        }

        /*
         * Returning to active roster.
         */
        if (
            "ACT".equals(after) &&
            (
                "IR".equals(before) ||
                "IRD".equals(before) ||
                "PUP".equals(before) ||
                "NON".equals(before)
            )
        ) {
            return "ACTIVATED";
        }

        /*
         * Waivers
         */
        if (
            code.startsWith("WA") ||
            type.contains("waiv") ||
            desc.contains("waived")
        ) {
            return "WAIVED";
        }

        /*
         * Releases
         */
        if (
            "REL".equals(code) ||
            type.contains("release") ||
            desc.contains("released")
        ) {
            return "RELEASED";
        }

        /*
         * Trades
         */
        if (
            "TRD".equals(code) ||
            type.contains("trade") ||
            desc.contains("traded")
        ) {
            return "TRADE";
        }

        /*
         * Signings
         */
        if (
            "SGN".equals(code) ||
            type.contains("sign") ||
            desc.contains("signed")
        ) {
            return "SIGNED";
        }

        /*
         * Retirement
         */
        if (
            "RET".equals(after) ||
            desc.contains("retired")
        ) {
            return "RETIRED";
        }

        /*
         * Generic activation.
         */
        if (
            "ACT".equals(code) ||
            type.contains("activated")
        ) {
            return "ACTIVATED";
        }

        return null;
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