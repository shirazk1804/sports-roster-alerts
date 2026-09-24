package sportsalerts;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;

public class NflTransactionServiceTest {

    @Test
    void parsesWaivedInjuryTransaction() {

        ObjectMapper objectMapper = new ObjectMapper();

        NflTransactionService service = new NflTransactionService(
                objectMapper,
                "test-key",
                new SportradarRequestLimiter());

        String jetsTeamId = "5fee86ae-74ab-4bdd-8416-42a9dd9964f3";

        String json = """
                {
                  "players": [
                    {
                      "id": "22b01678-952c-4923-b1a9-1249aa1e0791",
                      "name": "Kobe King",
                      "transactions": [
                        {
                          "id": "040aff10-a193-11f1-ace0-dfb38b033194",
                          "desc": "The New York Jets waived LB Kobe King due to injury.",
                          "effective_date": "2026-08-26",
                          "transaction_type": "Waived-Injury",
                          "transaction_code": "WAI",
                          "status_before": "IR",
                          "status_after": "UFA",
                          "from_team": {
                            "id": "5fee86ae-74ab-4bdd-8416-42a9dd9964f3",
                            "name": "Jets",
                            "market": "New York",
                            "alias": "NYJ"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        List<NflTransactionEvent> events = service
                .getNormalizedTransactionsForTeam(
                        json,
                        jetsTeamId);

        assertEquals(
                1,
                events.size());

        NflTransactionEvent event = events.get(0);

        assertEquals(
                "WAIVED",
                event.eventType());

        assertEquals(
                "Kobe King",
                event.playerName());

        assertEquals(
                "New York Jets",
                event.teamName());

        assertEquals(
                "2026-08-26",
                event.effectiveDate());

        assertEquals(
                "IR",
                event.statusBefore());

        assertEquals(
                "UFA",
                event.statusAfter());
    }

    @Test
    void parsesInjuredReserveTransaction() {

        ObjectMapper objectMapper = new ObjectMapper();

        NflTransactionService service = new NflTransactionService(
                objectMapper,
                "test-key",
                new SportradarRequestLimiter());

        String ravensTeamId = "ebd87119-b331-4469-9ea6-d51fe3ce2f1c";

        String json = """
                {
                  "players": [
                    {
                      "id": "22febe45-170a-4f01-b144-3b35b44c7da4",
                      "name": "Danny Pinter",
                      "transactions": [
                        {
                          "id": "0a53dfb0-a15f-11f1-929a-d322de85f75f",
                          "desc": "The Baltimore Ravens placed C Danny Pinter on IR.",
                          "effective_date": "2026-08-26",
                          "transaction_type": "Placed on Injured Reserve",
                          "transaction_code": "IR",
                          "status_before": "ACT",
                          "status_after": "IR",
                          "from_team": {
                            "id": "ebd87119-b331-4469-9ea6-d51fe3ce2f1c",
                            "name": "Ravens",
                            "market": "Baltimore",
                            "alias": "BAL"
                          }
                        }
                      ]
                    }
                  ]
                }
                """;

        List<NflTransactionEvent> events = service
                .getNormalizedTransactionsForTeam(
                        json,
                        ravensTeamId);

        assertEquals(
                1,
                events.size());

        NflTransactionEvent event = events.get(0);

        assertEquals(
                "INJURED_RESERVE",
                event.eventType());

        assertEquals(
                "Danny Pinter",
                event.playerName());

        assertEquals(
                "Baltimore Ravens",
                event.teamName());

        assertEquals(
                "ACT",
                event.statusBefore());

        assertEquals(
                "IR",
                event.statusAfter());
    }
}