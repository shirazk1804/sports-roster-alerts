package sportsalerts;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NflTransactionScheduler {

    private final FollowedTeamRepository followedTeamRepository;

    private final TeamRepository teamRepository;

    private final NflTransactionService nflTransactionService;

    private final RosterEventService rosterEventService;

    private final PushNotificationService pushNotificationService;

    public NflTransactionScheduler(
            FollowedTeamRepository followedTeamRepository,
            TeamRepository teamRepository,
            NflTransactionService nflTransactionService,
            RosterEventService rosterEventService,
            PushNotificationService pushNotificationService) {
        this.followedTeamRepository = followedTeamRepository;

        this.teamRepository = teamRepository;

        this.nflTransactionService = nflTransactionService;

        this.rosterEventService = rosterEventService;

        this.pushNotificationService = pushNotificationService;
    }

    @Scheduled(initialDelay = 20000, fixedDelay = 3600000)
    public void checkNflTransactions() {

        List<FollowedTeam> followedNflTeams = followedTeamRepository
                .findAll()
                .stream()
                .filter(
                        followedTeam -> "NFL".equals(
                                followedTeam.getLeague())
                                &&
                                followedTeam.getAppUser() != null)
                .toList();

        if (followedNflTeams.isEmpty()) {
            return;
        }

        /*
         * Group followers by NFL team.
         *
         * Rams
         * User 1
         * User 2
         *
         * Chiefs
         * User 3
         */
        Map<String, List<FollowedTeam>> followersByTeam = followedNflTeams
                .stream()
                .collect(
                        Collectors.groupingBy(
                                FollowedTeam::getName));

        LocalDate today = LocalDate.now(
                ZoneId.of("America/New_York"));

        String rawTransactions;

        try {
            /*
             * NFL Daily Transactions is
             * league-wide, so make ONE provider
             * request and reuse it for every
             * followed NFL team.
             */
            rawTransactions = nflTransactionService
                    .getDailyTransactions(
                            today);

        } catch (Exception exception) {

            System.err.println(
                    "NFL transaction request failed: "
                            + exception.getMessage());

            return;
        }

        int totalNewEvents = 0;
        int totalNotifications = 0;

        for (Map.Entry<String, List<FollowedTeam>> entry : followersByTeam.entrySet()) {
            String teamName = entry.getKey();

            List<FollowedTeam> followers = entry.getValue();

            Team team = teamRepository
                    .findByLeagueAndName(
                            "NFL",
                            teamName)
                    .orElse(null);

            if (team == null ||
                    team.getExternalProviderId() == null ||
                    team.getExternalProviderId()
                            .isBlank()) {
                System.err.println(
                        "No Sportradar NFL team ID found for: "
                                + teamName);

                continue;
            }

            String providerTeamId = team.getExternalProviderId();

            try {
                List<NflTransactionEvent> events = nflTransactionService
                        .getNormalizedTransactionsForTeam(
                                rawTransactions,
                                providerTeamId);

                /*
                 * Save each real NFL transaction
                 * only once per team.
                 */
                List<RosterEvent> savedEvents = rosterEventService
                        .saveNflEvents(
                                providerTeamId,
                                events);

                totalNewEvents += savedEvents.size();

                /*
                 * Then distribute each newly saved
                 * event to users following that team.
                 */
                for (RosterEvent event : savedEvents) {
                    for (FollowedTeam followedTeam : followers) {
                        AppUser user = followedTeam.getAppUser();

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
                        "NFL processing failed for "
                                + teamName
                                + ": "
                                + exception.getMessage());
            }
        }

        System.out.println(
                "NFL transaction check complete. "
                        + "Followed teams: "
                        + followersByTeam.size()
                        + ". New events: "
                        + totalNewEvents
                        + ". Notifications sent: "
                        + totalNotifications);
    }
}