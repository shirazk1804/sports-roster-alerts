package sportsalerts;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NflInjuryScheduler {

    private final FollowedTeamRepository
        followedTeamRepository;

    private final TeamRepository
        teamRepository;

    private final NflInjuryService
        nflInjuryService;

    private final NflInjuryChangeService
        nflInjuryChangeService;

    private final RosterEventService
        rosterEventService;

    private final PushNotificationService
        pushNotificationService;

    public NflInjuryScheduler(
        FollowedTeamRepository followedTeamRepository,
        TeamRepository teamRepository,
        NflInjuryService nflInjuryService,
        NflInjuryChangeService nflInjuryChangeService,
        RosterEventService rosterEventService,
        PushNotificationService pushNotificationService
    ) {
        this.followedTeamRepository =
            followedTeamRepository;

        this.teamRepository =
            teamRepository;

        this.nflInjuryService =
            nflInjuryService;

        this.nflInjuryChangeService =
            nflInjuryChangeService;

        this.rosterEventService =
            rosterEventService;

        this.pushNotificationService =
            pushNotificationService;
    }

    @Scheduled(
        initialDelay = 30000,
        fixedDelay = 3600000
    )
    public void checkNflInjuries() {

        List<FollowedTeam> followedNflTeams =
            followedTeamRepository
                .findAll()
                .stream()
                .filter(
                    followedTeam ->
                        "NFL".equals(
                            followedTeam.getLeague()
                        )
                        &&
                        followedTeam.getAppUser()
                            != null
                )
                .toList();

        if (followedNflTeams.isEmpty()) {
            return;
        }

        Map<String, List<FollowedTeam>>
            followersByTeam =
                followedNflTeams
                    .stream()
                    .collect(
                        Collectors.groupingBy(
                            FollowedTeam::getName
                        )
                    );

        NflWeekInfo currentWeek;
        String rawInjuries;

        try {
            /*
             * Determine the current NFL season,
             * season type, and week automatically.
             */
            currentWeek =
                nflInjuryService
                    .getCurrentWeek();

            /*
             * Weekly Injuries is league-wide,
             * so make one request and reuse it
             * for every followed NFL team.
             */
            rawInjuries =
                nflInjuryService
                    .getWeeklyInjuries(
                        currentWeek
                    );

        } catch (Exception exception) {

            System.err.println(
                "NFL injury request failed: "
                    + exception.getMessage()
            );

            return;
        }

        int totalInjuriesSeen = 0;
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
                        "NFL",
                        teamName
                    )
                    .orElse(null);

            if (
                team == null ||
                team.getExternalProviderId()
                    == null ||
                team.getExternalProviderId()
                    .isBlank()
            ) {
                System.err.println(
                    "No Sportradar NFL team ID found for injury check: "
                        + teamName
                );

                continue;
            }

            String providerTeamId =
                team.getExternalProviderId();

            try {
                List<NflInjuryEvent> injuries =
                    nflInjuryService
                        .getNormalizedInjuriesForTeam(
                            rawInjuries,
                            providerTeamId
                        );

                totalInjuriesSeen +=
                    injuries.size();

                /*
                 * First-ever observations become
                 * silent baselines.
                 *
                 * Only actual changes return
                 * RosterEvents.
                 */
                List<RosterEvent> newEvents =
                    nflInjuryChangeService
                        .processTeamInjuries(
                            providerTeamId,
                            injuries
                        );

                totalNewEvents +=
                    newEvents.size();

                for (
                    RosterEvent event :
                    newEvents
                ) {
                    for (
                        FollowedTeam followedTeam :
                        followers
                    ) {
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

            } catch (Exception exception) {

                System.err.println(
                    "NFL injury processing failed for "
                        + teamName
                        + ": "
                        + exception.getMessage()
                );
            }
        }

        System.out.println(
            "NFL injury check complete. "
                + "Season: "
                + currentWeek.seasonYear()
                + " "
                + currentWeek.seasonType()
                + " Week "
                + currentWeek.week()
                + ". Followed teams: "
                + followersByTeam.size()
                + ". Injuries seen: "
                + totalInjuriesSeen
                + ". New events: "
                + totalNewEvents
                + ". Notifications sent: "
                + totalNotifications
        );
    }
}