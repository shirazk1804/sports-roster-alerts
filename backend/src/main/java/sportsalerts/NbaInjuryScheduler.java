package sportsalerts;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class NbaInjuryScheduler {

        private final FollowedTeamRepository followedTeamRepository;

        private final TeamRepository teamRepository;

        private final NbaInjuryService nbaInjuryService;

        private final NbaInjuryChangeService nbaInjuryChangeService;

        private final RosterEventService rosterEventService;

        private final PushNotificationService pushNotificationService;

        private final NbaHeadshotService nbaHeadshotService;

        public NbaInjuryScheduler(
                        FollowedTeamRepository followedTeamRepository,
                        TeamRepository teamRepository,
                        NbaInjuryService nbaInjuryService,
                        NbaInjuryChangeService nbaInjuryChangeService,
                        RosterEventService rosterEventService,
                        PushNotificationService pushNotificationService,
                        NbaHeadshotService nbaHeadshotService) {

                this.followedTeamRepository = followedTeamRepository;

                this.teamRepository = teamRepository;

                this.nbaInjuryService = nbaInjuryService;

                this.nbaInjuryChangeService = nbaInjuryChangeService;

                this.rosterEventService = rosterEventService;

                this.pushNotificationService = pushNotificationService;

                this.nbaHeadshotService = nbaHeadshotService;
        }

        @Scheduled(initialDelay = 60000, fixedDelay = 14400000)
        public void checkNbaInjuries() {

                List<FollowedTeam> followedNbaTeams = followedTeamRepository
                                .findAll()
                                .stream()
                                .filter(
                                                team -> "NBA".equals(
                                                                team.getLeague())
                                                                &&
                                                                team.getAppUser() != null)
                                .toList();

                if (followedNbaTeams.isEmpty()) {
                        return;
                }

                Map<String, List<FollowedTeam>> followersByTeam = followedNbaTeams
                                .stream()
                                .collect(
                                                Collectors.groupingBy(
                                                                FollowedTeam::getName));

                String rawInjuries;

                try {

                        /*
                         * One league-wide request for all
                         * followed NBA teams.
                         */
                        rawInjuries = nbaInjuryService
                                        .getLeagueInjuries();

                } catch (Exception exception) {

                        System.err.println(
                                        "NBA injury request failed: "
                                                        + exception.getMessage());

                        return;
                }

                int totalInjuriesSeen = 0;
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
                                                "No Sportradar NBA team ID found for injury check: "
                                                                + teamName);

                                continue;
                        }

                        String providerTeamId = team.getExternalProviderId();

                        try {

                                List<NbaInjuryEvent> injuries = nbaInjuryService
                                                .getNormalizedInjuriesForTeam(
                                                                rawInjuries,
                                                                providerTeamId);

                                /*
                                 * Refresh current team-specific headshots
                                 * in the background scheduler.
                                 *
                                 * This never runs while a user is waiting
                                 * for the injury API response.
                                 */
                                for (NbaInjuryEvent injury : injuries) {

                                        try {

                                                nbaHeadshotService
                                                                .refreshHeadshot(
                                                                                teamName,
                                                                                injury.nbaPlayerId(),
                                                                                injury.playerName());

                                        } catch (Exception exception) {

                                                System.err.println(
                                                                "NBA headshot refresh failed for "
                                                                                + injury.playerName()
                                                                                + ": "
                                                                                + exception.getMessage());
                                        }
                                }

                                totalInjuriesSeen += injuries.size();

                                List<RosterEvent> newEvents = nbaInjuryChangeService
                                                .processTeamInjuries(
                                                                providerTeamId,
                                                                injuries);

                                totalNewEvents += newEvents.size();

                                for (RosterEvent event : newEvents) {

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
                                                "NBA injury processing failed for "
                                                                + teamName
                                                                + ": "
                                                                + exception.getMessage());
                        }
                }

                System.out.println(
                                "NBA injury check complete. "
                                                + "Followed teams: "
                                                + followersByTeam.size()
                                                + ". Injuries seen: "
                                                + totalInjuriesSeen
                                                + ". New events: "
                                                + totalNewEvents
                                                + ". Notifications sent: "
                                                + totalNotifications);
        }
}