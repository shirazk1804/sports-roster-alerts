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
         * Group followers by team so each MLB team
         * is checked only once per scheduler cycle,
         * regardless of how many users follow it.
         *
         * Example:
         *
         * Dodgers
         *   User 1
         *   User 2
         *
         * Yankees
         *   User 3
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

        String todayString =
            today.toString();

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

            Long externalTeamId =
                team.getExternalTeamId();

            try {

                /*
                 * IMPORTANT:
                 *
                 * Fetch only this team's transactions
                 * instead of downloading the entire
                 * league-wide transaction feed.
                 */
                List<MlbTransactionEvent> events =
                    mlbTransactionService
                        .getNormalizedTransactions(
                            externalTeamId,
                            todayString,
                            todayString
                        );

                /*
                 * roster_events remains global.
                 * Each real MLB event is saved only once.
                 */
                List<RosterEvent> savedEvents =
                    rosterEventService
                        .saveMlbEvents(
                            externalTeamId,
                            events
                        );

                totalNewEvents +=
                    savedEvents.size();

                /*
                 * Distribute newly saved events to
                 * each user following this team.
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