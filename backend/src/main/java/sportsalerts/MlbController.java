package sportsalerts;

import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mlb")
public class MlbController {

    private final MlbTransactionService
        mlbTransactionService;

    private final RosterEventService
        rosterEventService;

    public MlbController(
        MlbTransactionService
            mlbTransactionService,
        RosterEventService
            rosterEventService
    ) {
        this.mlbTransactionService =
            mlbTransactionService;

        this.rosterEventService =
            rosterEventService;
    }

    @GetMapping(
        value = "/transactions",
        produces =
            MediaType.APPLICATION_JSON_VALUE
    )
    public String getTransactions(
        @RequestParam Long teamId,
        @RequestParam String startDate,
        @RequestParam String endDate
    ) {
        return mlbTransactionService
            .getTransactions(
                teamId,
                startDate,
                endDate
            );
    }

    @GetMapping(
        "/transactions/normalized"
    )
    public List<MlbTransactionEvent>
        getNormalizedTransactions(
            @RequestParam Long teamId,
            @RequestParam String startDate,
            @RequestParam String endDate
        ) {

        return mlbTransactionService
            .getNormalizedTransactions(
                teamId,
                startDate,
                endDate
            );
    }

    @PostMapping(
        "/transactions/import"
    )
    public List<RosterEvent>
        importTransactions(
            @RequestParam Long teamId,
            @RequestParam String startDate,
            @RequestParam String endDate
        ) {

        List<MlbTransactionEvent> events =
            mlbTransactionService
                .getNormalizedTransactions(
                    teamId,
                    startDate,
                    endDate
                );

        return rosterEventService
            .saveMlbEvents(
                teamId,
                events
            );
    }
}