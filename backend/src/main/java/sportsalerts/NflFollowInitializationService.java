package sportsalerts;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class NflFollowInitializationService {

    private final TeamRepository
        teamRepository;

    private final NflTransactionService
        nflTransactionService;

    private final NflInjuryService
        nflInjuryService;

    private final NflInjuryChangeService
        nflInjuryChangeService;

    private final NflInjuryPracticeReportService
        nflInjuryPracticeReportService;

    private final NflInjurySnapshotRepository
        nflInjurySnapshotRepository;

    private final RosterEventService
        rosterEventService;

    public NflFollowInitializationService(
        TeamRepository teamRepository,
        NflTransactionService nflTransactionService,
        NflInjuryService nflInjuryService,
        NflInjuryChangeService nflInjuryChangeService,
        NflInjuryPracticeReportService nflInjuryPracticeReportService,
        NflInjurySnapshotRepository nflInjurySnapshotRepository,
        RosterEventService rosterEventService
    ) {
        this.teamRepository =
            teamRepository;

        this.nflTransactionService =
            nflTransactionService;

        this.nflInjuryService =
            nflInjuryService;

        this.nflInjuryChangeService =
            nflInjuryChangeService;

        this.nflInjuryPracticeReportService =
            nflInjuryPracticeReportService;

        this.nflInjurySnapshotRepository =
            nflInjurySnapshotRepository;

        this.rosterEventService =
            rosterEventService;
    }

    @Async
    public void initialize(
        String teamName
    ) {

        backfillTransactions(
            teamName
        );

        initializeInjuries(
            teamName
        );
    }

    private void backfillTransactions(
        String teamName
    ) {

        Team team =
            teamRepository
                .findByLeagueAndName(
                    "NFL",
                    teamName
                )
                .orElse(null);

        if (
            team == null ||
            team.getExternalProviderId() == null ||
            team.getExternalProviderId()
                .isBlank()
        ) {
            System.err.println(
                "Could not backfill NFL transactions. "
                    + "No Sportradar team ID found for "
                    + teamName
            );

            return;
        }

        String providerTeamId =
            team.getExternalProviderId();

        LocalDate endDate =
            LocalDate.now(
                ZoneId.of(
                    "America/New_York"
                )
            );

        LocalDate startDate =
            endDate.minusDays(6);

        int totalSaved = 0;
        int failedDays = 0;

        for (
            LocalDate date = startDate;
            !date.isAfter(endDate);
            date = date.plusDays(1)
        ) {
            try {
                String rawTransactions =
                    nflTransactionService
                        .getDailyTransactions(
                            date
                        );

                List<NflTransactionEvent> events =
                    nflTransactionService
                        .getNormalizedTransactionsForTeam(
                            rawTransactions,
                            providerTeamId
                        );

                List<RosterEvent> savedEvents =
                    rosterEventService
                        .saveNflEvents(
                            providerTeamId,
                            events
                        );

                totalSaved +=
                    savedEvents.size();

            } catch (Exception exception) {

                failedDays++;

                System.err.println(
                    "NFL transaction backfill failed for "
                        + teamName
                        + " on "
                        + date
                        + ": "
                        + exception.getMessage()
                );
            }
        }

        System.out.println(
            "NFL transaction backfill complete for "
                + teamName
                + ". Saved "
                + totalSaved
                + " recent events. Failed days: "
                + failedDays
                + "."
        );
    }

    private void initializeInjuries(
        String teamName
    ) {

        Team team =
            teamRepository
                .findByLeagueAndName(
                    "NFL",
                    teamName
                )
                .orElse(null);

        if (
            team == null ||
            team.getExternalProviderId() == null ||
            team.getExternalProviderId()
                .isBlank()
        ) {
            System.err.println(
                "Could not initialize NFL injuries. "
                    + "No Sportradar team ID found for "
                    + teamName
            );

            return;
        }

        String providerTeamId =
            team.getExternalProviderId();

        try {
            NflWeekInfo week =
                nflInjuryService
                    .getUpcomingWeek();

            String rawInjuries =
                nflInjuryService
                    .getWeeklyInjuries(
                        week
                    );

            List<NflInjuryEvent> injuries =
                nflInjuryService
                    .getNormalizedInjuriesForTeam(
                        rawInjuries,
                        providerTeamId
                    );

            nflInjuryPracticeReportService
                .captureReports(
                    providerTeamId,
                    week,
                    injuries
                );

            boolean hasCurrentWeekSnapshot =
                nflInjurySnapshotRepository
                    .findByExternalProviderTeamId(
                        providerTeamId
                    )
                    .stream()
                    .anyMatch(
                        snapshot ->
                            snapshot.getSeasonYear() != null
                            &&
                            snapshot.getSeasonYear()
                                .equals(
                                    week.seasonYear()
                                )
                            &&
                            snapshot.getSeasonType() != null
                            &&
                            snapshot.getSeasonType()
                                .equals(
                                    week.seasonType()
                                )
                            &&
                            snapshot.getWeekNumber() != null
                            &&
                            snapshot.getWeekNumber()
                                .equals(
                                    week.week()
                                )
                    );

            if (!hasCurrentWeekSnapshot) {
                nflInjuryChangeService
                    .processTeamInjuries(
                        providerTeamId,
                        week,
                        injuries
                    );
            }

            System.out.println(
                "NFL injury initialization complete for "
                    + teamName
                    + ". Injuries found: "
                    + injuries.size()
            );

        } catch (Exception exception) {

            System.err.println(
                "NFL injury initialization failed for "
                    + teamName
                    + ": "
                    + exception.getMessage()
            );
        }
    }
}