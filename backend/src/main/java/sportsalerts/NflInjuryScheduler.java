package sportsalerts;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

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

        /*
         * First determine the actual upcoming
         * NFL week from the full schedule.
         */
        NflWeekInfo currentWeek;

        try {
            currentWeek =
                nflInjuryService
                    .getUpcomingWeek();

        } catch (Exception exception) {

            System.err.println(
                "Could not determine upcoming NFL week: "
                    + exception.getMessage()
            );

            return;
        }

        /*
         * Resolve provider IDs once and clear
         * stale snapshots BEFORE attempting to
         * fetch this week's injury report.
         */
        Map<String, String>
            providerTeamIdsByName =
                new HashMap<>();

        int staleSnapshotsRemoved = 0;

        for (
            String teamName :
            followersByTeam.keySet()
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

            providerTeamIdsByName.put(
                teamName,
                providerTeamId
            );

            try {
                staleSnapshotsRemoved +=
                    nflInjuryChangeService
                        .clearStaleSnapshots(
                            providerTeamId,
                            currentWeek
                        );

            } catch (Exception exception) {

                System.err.println(
                    "Could not clear stale NFL injury snapshots for "
                        + teamName
                        + ": "
                        + exception.getMessage()
                );
            }
        }

        /*
         * Now try to fetch the upcoming week's
         * injury report.
         *
         * If Sportradar has not published it yet,
         * the old week's snapshots are already
         * gone, so the app won't show stale data.
         */
        String rawInjuries;

        try {
            rawInjuries =
                nflInjuryService
                    .getWeeklyInjuries(
                        currentWeek
                    );

        } catch (
            HttpClientErrorException.NotFound exception
        ) {
            System.out.println(
                "NFL injury report not available yet. "
                    + "Season: "
                    + currentWeek.seasonYear()
                    + " "
                    + currentWeek.seasonType()
                    + " Week "
                    + currentWeek.week()
                    + ". Stale snapshots removed: "
                    + staleSnapshotsRemoved
            );

            return;

        } catch (Exception exception) {

            System.err.println(
                "NFL injury request failed for "
                    + currentWeek.seasonYear()
                    + " "
                    + currentWeek.seasonType()
                    + " Week "
                    + currentWeek.week()
                    + ": "
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

            String providerTeamId =
                providerTeamIdsByName.get(
                    teamName
                );

            if (providerTeamId == null) {
                continue;
            }

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
                 * First observations for the
                 * upcoming week become silent
                 * baselines.
                 *
                 * Only later status changes
                 * create RosterEvents.
                 */
                List<RosterEvent> newEvents =
                    nflInjuryChangeService
                        .processTeamInjuries(
                            providerTeamId,
                            currentWeek,
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
                + ". Stale snapshots removed: "
                + staleSnapshotsRemoved
                + ". New events: "
                + totalNewEvents
                + ". Notifications sent: "
                + totalNotifications
        );
    }
}