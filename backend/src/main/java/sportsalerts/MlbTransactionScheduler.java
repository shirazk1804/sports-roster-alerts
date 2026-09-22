package sportsalerts;

import java.time.LocalDate;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MlbTransactionScheduler {

    private final FollowedTeamRepository
        followedTeamRepository;

    private final TeamRepository
        teamRepository;

    private final MlbTransactionService
        mlbTransactionService;

    private final RosterEventService
        rosterEventService;

    private final PushNotificationService
        pushNotificationService;

    public MlbTransactionScheduler(
        FollowedTeamRepository followedTeamRepository,
        TeamRepository teamRepository,
        MlbTransactionService mlbTransactionService,
        RosterEventService rosterEventService,
        PushNotificationService pushNotificationService
    ) {
        this.followedTeamRepository =
            followedTeamRepository;

        this.teamRepository =
            teamRepository;

        this.mlbTransactionService =
            mlbTransactionService;

        this.rosterEventService =
            rosterEventService;

        this.pushNotificationService =
            pushNotificationService;
    }

    @Scheduled(
        initialDelay = 10000,
        fixedDelay = 5000
    )
    public void checkMlbTransactions() {

        List<FollowedTeam> followedMlbTeams =
            followedTeamRepository
                .findAll()
                .stream()
                .filter(
                    team ->
                        "MLB".equals(
                            team.getLeague()
                        )
                )
                .toList();

        if (followedMlbTeams.isEmpty()) {
            return;
        }

        LocalDate today =
            LocalDate.now();

        String rawTransactions;

        try {
            rawTransactions =
                mlbTransactionService
                    .getAllMlbTransactions(
                        today.toString(),
                        today.toString()
                    );

        } catch (Exception exception) {
            System.err.println(
                "MLB live transaction request failed: "
                + exception.getMessage()
            );

            return;
        }

        int totalNewEvents = 0;
        int totalNotifications = 0;

        for (
            FollowedTeam followedTeam :
            followedMlbTeams
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
                    "No MLB team ID found for: "
                    + followedTeam.getName()
                );

                continue;
            }

            try {
                List<MlbTransactionEvent> events =
                    mlbTransactionService
                        .getNormalizedTransactionsForTeam(
                            rawTransactions,
                            team.getExternalTeamId()
                        );

                List<RosterEvent> savedEvents =
                    rosterEventService
                        .saveMlbEvents(
                            team.getExternalTeamId(),
                            events
                        );

                totalNewEvents +=
                    savedEvents.size();

                for (
                    RosterEvent event :
                    savedEvents
                ) {
                    if (
                        rosterEventService
                            .shouldNotify(
                                followedTeam,
                                event
                            )
                    ) {
                        pushNotificationService
                            .sendRosterEventNotification(
                                event
                            );

                        totalNotifications++;
                    }
                }

                if (!savedEvents.isEmpty()) {
                    System.out.println(
                        "MLB live check: "
                        + followedTeam.getName()
                        + " - New events: "
                        + savedEvents.size()
                    );
                }

            } catch (Exception exception) {
                System.err.println(
                    "MLB processing failed for "
                    + followedTeam.getName()
                    + ": "
                    + exception.getMessage()
                );
            }
        }

        if (
            totalNewEvents > 0 ||
            totalNotifications > 0
        ) {
            System.out.println(
                "MLB live update complete. "
                + "New events: "
                + totalNewEvents
                + ". Notifications sent: "
                + totalNotifications
            );
        }
    }
}