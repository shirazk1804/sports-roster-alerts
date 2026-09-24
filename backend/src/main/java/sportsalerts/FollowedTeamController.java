package sportsalerts;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/followed-teams")
public class FollowedTeamController {

    private final FollowedTeamRepository
        followedTeamRepository;

    private final AlertPreferenceRepository
        alertPreferenceRepository;

    private final TeamRepository
        teamRepository;

    private final MlbTransactionService
        mlbTransactionService;

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

    private final AppUserService
        appUserService;

    public FollowedTeamController(
        FollowedTeamRepository followedTeamRepository,
        AlertPreferenceRepository alertPreferenceRepository,
        TeamRepository teamRepository,
        MlbTransactionService mlbTransactionService,
        NflTransactionService nflTransactionService,
        NflInjuryService nflInjuryService,
        NflInjuryChangeService nflInjuryChangeService,
        NflInjuryPracticeReportService nflInjuryPracticeReportService,
        NflInjurySnapshotRepository nflInjurySnapshotRepository,
        RosterEventService rosterEventService,
        AppUserService appUserService
    ) {
        this.followedTeamRepository =
            followedTeamRepository;

        this.alertPreferenceRepository =
            alertPreferenceRepository;

        this.teamRepository =
            teamRepository;

        this.mlbTransactionService =
            mlbTransactionService;

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

        this.appUserService =
            appUserService;
    }

    @GetMapping
    public List<FollowedTeam> getFollowedTeams(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        String authorizationHeader
    ) {
        AppUser user =
            appUserService
                .requireAuthenticatedUser(
                    authorizationHeader
                );

        return followedTeamRepository
            .findByAppUserId(
                user.getId()
            );
    }

    @PostMapping
    public FollowedTeam followTeam(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        String authorizationHeader,
        @RequestBody FollowedTeam followedTeam
    ) {
        AppUser user =
            appUserService
                .requireAuthenticatedUser(
                    authorizationHeader
                );

        boolean alreadyFollowing =
            followedTeamRepository
                .existsByAppUserIdAndLeagueAndName(
                    user.getId(),
                    followedTeam.getLeague(),
                    followedTeam.getName()
                );

        if (alreadyFollowing) {
            return followedTeamRepository
                .findByAppUserIdAndLeagueAndName(
                    user.getId(),
                    followedTeam.getLeague(),
                    followedTeam.getName()
                )
                .orElseThrow();
        }

        followedTeam.setAppUser(
            user
        );

        FollowedTeam savedTeam =
            followedTeamRepository.save(
                followedTeam
            );

        if (
            "MLB".equals(
                savedTeam.getLeague()
            )
        ) {
            backfillMlbTransactions(
                savedTeam
            );
        }

        if (
            "NFL".equals(
                savedTeam.getLeague()
            )
        ) {
            backfillNflTransactions(
                savedTeam
            );

            initializeNflInjuries(
                savedTeam
            );
        }

        return savedTeam;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void unfollowTeam(
        @PathVariable Long id,
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        String authorizationHeader
    ) {
        AppUser user =
            appUserService
                .requireAuthenticatedUser(
                    authorizationHeader
                );

        FollowedTeam followedTeam =
            followedTeamRepository
                .findById(id)
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Followed team not found"
                        )
                );

        if (
            followedTeam.getAppUser() == null ||
            !followedTeam
                .getAppUser()
                .getId()
                .equals(user.getId())
        ) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Followed team not found"
            );
        }

        alertPreferenceRepository
            .deleteByFollowedTeamId(id);

        followedTeamRepository
            .delete(followedTeam);
    }

    // =========================
    // MLB BACKFILL
    // =========================

    private void backfillMlbTransactions(
        FollowedTeam followedTeam
    ) {
        Team team =
            teamRepository
                .findByLeagueAndName(
                    "MLB",
                    followedTeam.getName()
                )
                .orElse(null);

        if (
            team == null ||
            team.getExternalTeamId() == null
        ) {
            System.err.println(
                "Could not backfill MLB transactions. "
                    + "No external team ID found for "
                    + followedTeam.getName()
            );

            return;
        }

        LocalDate endDate =
            LocalDate.now();

        LocalDate startDate =
            endDate.minusDays(7);

        try {
            List<MlbTransactionEvent> events =
                mlbTransactionService
                    .getNormalizedTransactions(
                        team.getExternalTeamId(),
                        startDate.toString(),
                        endDate.toString()
                    );

            List<RosterEvent> savedEvents =
                rosterEventService
                    .saveMlbEvents(
                        team.getExternalTeamId(),
                        events
                    );

            System.out.println(
                "MLB backfill complete for "
                    + followedTeam.getName()
                    + ". Saved "
                    + savedEvents.size()
                    + " recent events."
            );

        } catch (Exception exception) {
            System.err.println(
                "MLB backfill failed for "
                    + followedTeam.getName()
                    + ": "
                    + exception.getMessage()
            );
        }
    }

    // =========================
    // NFL TRANSACTION BACKFILL
    // =========================

    private void backfillNflTransactions(
        FollowedTeam followedTeam
    ) {
        Team team =
            teamRepository
                .findByLeagueAndName(
                    "NFL",
                    followedTeam.getName()
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
                    + followedTeam.getName()
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

        /*
         * Seven calendar days including today.
         */
        LocalDate startDate =
            endDate.minusDays(6);

        int totalSaved = 0;

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
                System.err.println(
                    "NFL transaction backfill failed for "
                        + followedTeam.getName()
                        + " on "
                        + date
                        + ": "
                        + exception.getMessage()
                );
            }
        }

        System.out.println(
            "NFL transaction backfill complete for "
                + followedTeam.getName()
                + ". Saved "
                + totalSaved
                + " recent events."
        );
    }

    // =========================
    // NFL INJURY INITIALIZATION
    // =========================

    private void initializeNflInjuries(
        FollowedTeam followedTeam
    ) {
        Team team =
            teamRepository
                .findByLeagueAndName(
                    "NFL",
                    followedTeam.getName()
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
                    + followedTeam.getName()
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

            /*
             * Always capture the current practice
             * report history.
             */
            nflInjuryPracticeReportService
                .captureReports(
                    providerTeamId,
                    week,
                    injuries
                );

            /*
             * If this team already has current-week
             * snapshots, another user's scheduler
             * has already initialized it.
             *
             * Do not process it here again because
             * follow/backfill should not generate
             * push-worthy injury changes.
             */
            boolean hasCurrentWeekSnapshot =
                nflInjurySnapshotRepository
                    .findByExternalProviderTeamId(
                        providerTeamId
                    )
                    .stream()
                    .anyMatch(
                        snapshot ->
                            snapshot.getSeasonYear()
                                != null
                            &&
                            snapshot.getSeasonYear()
                                .equals(
                                    week.seasonYear()
                                )
                            &&
                            snapshot.getSeasonType()
                                != null
                            &&
                            snapshot.getSeasonType()
                                .equals(
                                    week.seasonType()
                                )
                            &&
                            snapshot.getWeekNumber()
                                != null
                            &&
                            snapshot.getWeekNumber()
                                .equals(
                                    week.week()
                                )
                    );

            /*
             * No current snapshot means this team
             * has not yet been initialized for
             * this NFL week.
             *
             * processTeamInjuries will create a
             * silent baseline on first observation.
             */
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
                    + followedTeam.getName()
                    + ". Injuries found: "
                    + injuries.size()
            );

        } catch (Exception exception) {
            System.err.println(
                "NFL injury initialization failed for "
                    + followedTeam.getName()
                    + ": "
                    + exception.getMessage()
            );
        }
    }
}