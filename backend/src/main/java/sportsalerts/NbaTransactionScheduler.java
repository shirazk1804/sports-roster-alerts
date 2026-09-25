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

    private final FollowedTeamRepository
            followedTeamRepository;

    private final TeamRepository
            teamRepository;

    private final NbaTransactionService
            nbaTransactionService;

    private final RosterEventService
            rosterEventService;

    private final PushNotificationService
            pushNotificationService;

    public NbaTransactionScheduler(
            FollowedTeamRepository followedTeamRepository,
            TeamRepository teamRepository,
            NbaTransactionService nbaTransactionService,
            RosterEventService rosterEventService,
            PushNotificationService pushNotificationService) {

        this.followedTeamRepository =
                followedTeamRepository;

        this.teamRepository =
                teamRepository;

        this.nbaTransactionService =
                nbaTransactionService;

        this.rosterEventService =
                rosterEventService;

        this.pushNotificationService =
                pushNotificationService;
    }

    @Scheduled(
            initialDelay = 45000,
            fixedDelay = 3600000)
    public void checkNbaTransactions() {

        List<FollowedTeam> followedNbaTeams =
                followedTeamRepository
                        .findAll()
                        .stream()
                        .filter(
                                followedTeam ->
                                        "NBA".equals(
                                                followedTeam.getLeague())
                                                &&
                                        followedTeam.getAppUser()
                                                != null)
                        .toList();

        if (followedNbaTeams.isEmpty()) {
            return;
        }

        Map<String, List<FollowedTeam>>
                followersByTeam =
                followedNbaTeams
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        FollowedTeam::getName));

        LocalDate today =
                LocalDate.now(
                        ZoneId.of(
                                "America/New_York"));

        String rawTransfers;

        try {

            rawTransfers =
                    nbaTransactionService
                            .getDailyTransfers(
                                    today);

        } catch (Exception exception) {

            System.err.println(
                    "NBA transaction request failed: "
                            + exception.getMessage());

            return;
        }

        int totalNewEvents = 0;
        int totalNotifications = 0;

        for (
                Map.Entry<
                        String,
                        List<FollowedTeam>>
                entry :
                followersByTeam.entrySet()) {

            String teamName =
                    entry.getKey();

            List<FollowedTeam> followers =
                    entry.getValue();

            Team team =
                    teamRepository
                            .findByLeagueAndName(
                                    "NBA",
                                    teamName)
                            .orElse(null);

            if (team == null ||
                    team.getExternalProviderId()
                            == null ||
                    team.getExternalProviderId()
                            .isBlank()) {

                System.err.println(
                        "No Sportradar NBA team ID found for: "
                                + teamName);

                continue;
            }

            String providerTeamId =
                    team.getExternalProviderId();

            try {

                List<NbaTransactionEvent> events =
                        nbaTransactionService
                                .getNormalizedTransfersForTeam(
                                        rawTransfers,
                                        providerTeamId);

                List<RosterEvent> savedEvents =
                        rosterEventService
                                .saveNbaEvents(
                                        providerTeamId,
                                        events);

                totalNewEvents +=
                        savedEvents.size();

                for (
                        RosterEvent event :
                        savedEvents) {

                    for (
                            FollowedTeam followedTeam :
                            followers) {

                        AppUser user =
                                followedTeam
                                        .getAppUser();

                        if (user == null) {
                            continue;
                        }

                        boolean shouldNotify =
                                rosterEventService
                                        .shouldNotify(
                                                followedTeam,
                                                event);

                        if (!shouldNotify) {
                            continue;
                        }

                        int sent =
                                pushNotificationService
                                        .sendRosterEventNotification(
                                                user.getId(),
                                                event);

                        totalNotifications +=
                                sent;
                    }
                }

            } catch (Exception exception) {

                System.err.println(
                        "NBA processing failed for "
                                + teamName
                                + ": "
                                + exception.getMessage());
            }
        }

        System.out.println(
                "NBA transaction check complete. "
                        + "Followed teams: "
                        + followersByTeam.size()
                        + ". New events: "
                        + totalNewEvents
                        + ". Notifications sent: "
                        + totalNotifications);
    }
}