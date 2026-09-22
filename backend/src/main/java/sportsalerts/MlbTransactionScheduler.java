package sportsalerts;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
                    followedTeam ->
                        "MLB".equals(
                            followedTeam.getLeague()
                        )
                        &&
                        followedTeam.getAppUser()
                            != null
                )
                .toList();

        if (followedMlbTeams.isEmpty()) {
            return;
        }

        /*
         * Group users by team.
         *
         * Example:
         *
         * Dodgers
         *   User 1
         *   User 2
         *
         * Yankees
         *   User 3
         *
         * This lets us process each MLB team once
         * while still notifying every user who
         * follows that team.
         */
        Map<String, List<FollowedTeam>>
            followersByTeam =
                followedMlbTeams
                    .stream()
                    .collect(
                        Collectors.groupingBy(
                            FollowedTeam::getName
                        )
                    );

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
            Map.Entry<
                String,
                List<FollowedTeam>
            > entry :
            followersByTeam.entrySet()
        ) {
            String teamName =
                entry.getKey();

            List<FollowedTeam> followers =
                entry.getValue();

            Team team =
                teamRepository
                    .findByLeagueAndName(
                        "MLB",
                        teamName
                    )
                    .orElse(null);

            if (
                team == null ||
                team.getExternalTeamId() == null
            ) {
                System.err.println(
                    "No MLB team ID found for: "
                    + teamName
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

                /*
                 * Save each MLB event only once.
                 *
                 * roster_events is global event data,
                 * not one copy per user.
                 */
                List<RosterEvent> savedEvents =
                    rosterEventService
                        .saveMlbEvents(
                            team.getExternalTeamId(),
                            events
                        );

                totalNewEvents +=
                    savedEvents.size();

                /*
                 * Now distribute each newly saved event
                 * to every user following this team.
                 */
                for (
                    RosterEvent event :
                    savedEvents
                ) {
                    for (
                        FollowedTeam followedTeam :
                        followers
                    ) {
                        AppUser user =
                            followedTeam.getAppUser();

                        if (user == null) {
                            continue;
                        }

                        boolean shouldNotify =
                            rosterEventService
                                .shouldNotify(
                                    followedTeam,
                                    event
                                );

                        if (!shouldNotify) {
                            continue;
                        }

                        int sent =
                            pushNotificationService
                                .sendRosterEventNotification(
                                    user.getId(),
                                    event
                                );

                        totalNotifications +=
                            sent;
                    }
                }

                if (!savedEvents.isEmpty()) {

                    System.out.println(
                        "MLB live check: "
                        + teamName
                        + " - New events: "
                        + savedEvents.size()
                        + " - Followers: "
                        + followers.size()
                    );
                }

            } catch (Exception exception) {

                System.err.println(
                    "MLB processing failed for "
                    + teamName
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