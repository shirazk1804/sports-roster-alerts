package sportsalerts;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NbaTransactionScheduler {

    private record ProcessingResult(
            int newEvents,
            int notifications) {
    }

    private final FollowedTeamRepository followedTeamRepository;

    private final TeamRepository teamRepository;

    private final NbaTransactionService nbaTransactionService;

    private final RosterEventService rosterEventService;

    private final PushNotificationService pushNotificationService;

    private boolean initialBackfillComplete = false;

    public NbaTransactionScheduler(
            FollowedTeamRepository followedTeamRepository,
            TeamRepository teamRepository,
            NbaTransactionService nbaTransactionService,
            RosterEventService rosterEventService,
            PushNotificationService pushNotificationService) {

        this.followedTeamRepository = followedTeamRepository;

        this.teamRepository = teamRepository;

        this.nbaTransactionService = nbaTransactionService;

        this.rosterEventService = rosterEventService;

        this.pushNotificationService = pushNotificationService;
    }

    @Scheduled(initialDelay = 45000, fixedDelay = 3600000)
    @Scheduled(initialDelay = 45000, fixedDelay = 3600000)
    public void checkNbaTransactions() {

        List<FollowedTeam> followedNbaTeams = followedTeamRepository
                .findAll()
                .stream()
                .filter(
                        followedTeam -> "NBA".equals(
                                followedTeam.getLeague())
                                &&
                                followedTeam.getAppUser() != null)
                .toList();

        if (followedNbaTeams.isEmpty()) {
            return;
        }

        Map<String, List<FollowedTeam>> followersByTeam = followedNbaTeams
                .stream()
                .collect(
                        Collectors.groupingBy(
                                FollowedTeam::getName));

        LocalDate today = LocalDate.now(
                ZoneId.of(
                        "America/New_York"));

        int totalNewEvents = 0;
        int totalNotifications = 0;

        /*
         * On the first run after startup,
         * backfill the previous 7 days so
         * Recent Alerts includes moves that
         * happened before the app checked them.
         *
         * Historical events are saved but do
         * not trigger old push notifications.
         */
        if (!initialBackfillComplete) {

            for (int daysAgo = 6; daysAgo >= 1; daysAgo--) {

                LocalDate date = today.minusDays(
                        daysAgo);

                ProcessingResult result = processDate(
                        date,
                        followersByTeam,
                        false);

                totalNewEvents += result.newEvents();

                totalNotifications += result.notifications();
            }

            initialBackfillComplete = true;
        }

        /*
         * Always process today.
         *
         * New events discovered today may
         * send push notifications.
         */
        ProcessingResult todayResult = processDate(
                today,
                followersByTeam,
                true);

        totalNewEvents += todayResult.newEvents();

        totalNotifications += todayResult.notifications();

        System.out.println(
                "NBA transaction check complete. "
                        + "Followed teams: "
                        + followersByTeam.size()
                        + ". New events: "
                        + totalNewEvents
                        + ". Notifications sent: "
                        + totalNotifications);
    }

    private ProcessingResult processDate(
            LocalDate date,
            Map<String, List<FollowedTeam>> followersByTeam,
            boolean sendNotifications) {

        String rawTransfers;

        try {

            rawTransfers = nbaTransactionService
                    .getDailyTransfers(
                            date);

        } catch (Exception exception) {

            System.err.println(
                    "NBA transaction request failed for "
                            + date
                            + ": "
                            + exception.getMessage());

            return new ProcessingResult(
                    0,
                    0);
        }

        int totalNewEvents = 0;
        int totalNotifications = 0;

        for (Map.Entry<String, List<FollowedTeam>> entry : followersByTeam.entrySet()) {

            String teamName = entry.getKey();

            List<FollowedTeam> followers = entry.getValue();

            Team team = teamRepository
                    .findByLeagueAndName(
                            "NBA",
                            teamName)
                    .orElse(null);

            if (team == null ||
                    team.getExternalProviderId() == null ||
                    team.getExternalProviderId()
                            .isBlank()) {

                System.err.println(
                        "No Sportradar NBA team ID found for: "
                                + teamName);

                continue;
            }

            String providerTeamId = team.getExternalProviderId();

            try {

                List<NbaTransactionEvent> events = nbaTransactionService
                        .getNormalizedTransfersForTeam(
                                rawTransfers,
                                providerTeamId);

                List<RosterEvent> savedEvents = rosterEventService
                        .saveNbaEvents(
                                providerTeamId,
                                events);

                totalNewEvents += savedEvents.size();

                /*
                 * Historical backfill is for
                 * Recent Alerts only.
                 *
                 * Do not send push notifications
                 * for old transactions.
                 */
                if (!sendNotifications) {
                    continue;
                }

                for (RosterEvent event : savedEvents) {

                    for (FollowedTeam followedTeam : followers) {

                        AppUser user = followedTeam
                                .getAppUser();

                        if (user == null) {
                            continue;
                        }

                        boolean shouldNotify = rosterEventService
                                .shouldNotify(
                                        followedTeam,
                                        event);

                        if (!shouldNotify) {
                            continue;
                        }

                        int sent = pushNotificationService
                                .sendRosterEventNotification(
                                        user.getId(),
                                        event);

                        totalNotifications += sent;
                    }
                }

            } catch (Exception exception) {

                System.err.println(
                        "NBA processing failed for "
                                + teamName
                                + " on "
                                + date
                                + ": "
                                + exception.getMessage());
            }
        }

        return new ProcessingResult(
                totalNewEvents,
                totalNotifications);
    }
}