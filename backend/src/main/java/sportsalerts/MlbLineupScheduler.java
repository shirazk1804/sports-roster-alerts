package sportsalerts;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class MlbLineupScheduler {

    private final FollowedTeamRepository
            followedTeamRepository;

    private final TeamRepository
            teamRepository;

    private final MlbLineupService
            mlbLineupService;

    private final RosterEventService
            rosterEventService;

    private final PushNotificationService
            pushNotificationService;

    public MlbLineupScheduler(
            FollowedTeamRepository followedTeamRepository,
            TeamRepository teamRepository,
            MlbLineupService mlbLineupService,
            RosterEventService rosterEventService,
            PushNotificationService pushNotificationService) {

        this.followedTeamRepository =
                followedTeamRepository;

        this.teamRepository =
                teamRepository;

        this.mlbLineupService =
                mlbLineupService;

        this.rosterEventService =
                rosterEventService;

        this.pushNotificationService =
                pushNotificationService;
    }

    @Scheduled(
            initialDelay = 30000,
            fixedDelay = 60000)
    public void checkMlbLineups() {

        List<FollowedTeam>
                followedMlbTeams =
                followedTeamRepository
                        .findAll()
                        .stream()
                        .filter(
                                followedTeam ->
                                        "MLB".equals(
                                                followedTeam
                                                        .getLeague())
                                                &&
                                                followedTeam
                                                        .getAppUser()
                                                        != null)
                        .toList();

        if (followedMlbTeams.isEmpty()) {
            return;
        }

        /*
         * Only call MLB once per unique
         * followed team, even if several
         * users follow that team.
         */
        Map<String, List<FollowedTeam>>
                followersByTeam =
                followedMlbTeams
                        .stream()
                        .collect(
                                Collectors.groupingBy(
                                        FollowedTeam::getName));

        LocalDate today =
                LocalDate.now(
                        ZoneId.of(
                                "America/New_York"));

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
                                    "MLB",
                                    teamName)
                            .orElse(null);

            if (team == null ||
                    team.getExternalTeamId()
                            == null) {

                continue;
            }

            Long externalTeamId =
                    team.getExternalTeamId();

            try {

                MlbLineupResponse lineup =
                        mlbLineupService
                                .getLineup(
                                        externalTeamId,
                                        today);

                /*
                 * We only create an alert once
                 * the official batting order
                 * is actually available.
                 */
                if (!"POSTED".equals(
                        lineup.state()) ||
                        lineup.gamePk()
                                == null) {

                    continue;
                }

                /*
                 * The roster-event dedupe key
                 * contains the gamePk, so each
                 * lineup can only generate one
                 * alert.
                 */
                RosterEvent event =
                        rosterEventService
                                .saveMlbLineupPostedEvent(
                                        externalTeamId,
                                        teamName,
                                        lineup.gamePk(),
                                        lineup.opponentName(),
                                        lineup.homeAway(),
                                        today);

                if (event == null) {
                    continue;
                }

                int notificationsSent = 0;

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

                    notificationsSent +=
                            pushNotificationService
                                    .sendRosterEventNotification(
                                            user.getId(),
                                            event);
                }

                System.out.println(
                        "MLB lineup posted: "
                                + teamName
                                + " - Notifications sent: "
                                + notificationsSent);

            } catch (Exception exception) {

                System.err.println(
                        "MLB lineup check failed for "
                                + teamName
                                + ": "
                                + exception.getMessage());
            }
        }
    }
}