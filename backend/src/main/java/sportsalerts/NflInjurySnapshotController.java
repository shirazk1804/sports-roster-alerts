package sportsalerts;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/followed-teams")
public class NflInjurySnapshotController {

    private final FollowedTeamRepository followedTeamRepository;

    private final TeamRepository teamRepository;

    private final NflInjurySnapshotRepository snapshotRepository;

    private final NflInjuryPracticeReportRepository practiceReportRepository;

    private final AppUserService appUserService;

    private final NflPlayerImageService nflPlayerImageService;

    public NflInjurySnapshotController(
            FollowedTeamRepository followedTeamRepository,
            TeamRepository teamRepository,
            NflInjurySnapshotRepository snapshotRepository,
            NflInjuryPracticeReportRepository practiceReportRepository,
            AppUserService appUserService,
            NflPlayerImageService nflPlayerImageService) {
        this.followedTeamRepository = followedTeamRepository;

        this.teamRepository = teamRepository;

        this.snapshotRepository = snapshotRepository;

        this.practiceReportRepository = practiceReportRepository;

        this.appUserService = appUserService;

        this.nflPlayerImageService = nflPlayerImageService;
    }

    @GetMapping("/{followedTeamId}/injuries")
    public List<NflInjurySnapshotResponse> getCurrentInjuries(
            @PathVariable Long followedTeamId,

            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {

        AppUser user = appUserService
                .requireAuthenticatedUser(
                        authorizationHeader);

        FollowedTeam followedTeam = followedTeamRepository
                .findById(
                        followedTeamId)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND));

        /*
         * Do not expose another user's
         * followed-team data.
         */
        if (followedTeam.getAppUser() == null ||
                !followedTeam
                        .getAppUser()
                        .getId()
                        .equals(
                                user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND);
        }

        if (!"NFL".equals(
                followedTeam.getLeague())) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Current injury reports are only available for NFL teams");
        }

        Team team = teamRepository
                .findByLeagueAndName(
                        "NFL",
                        followedTeam.getName())
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "NFL team not found"));

        String providerTeamId = team.getExternalProviderId();

        if (providerTeamId == null ||
                providerTeamId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "NFL provider team ID not found");
        }

        List<NflInjurySnapshot> snapshots = snapshotRepository
                .findByExternalProviderTeamIdOrderByPlayerNameAsc(
                        providerTeamId);

        /*
         * No current injury report has been
         * published for this team/week yet.
         */
        if (snapshots.isEmpty()) {
            return List.of();
        }

        /*
         * Current snapshots are cleared when
         * the NFL week changes, so all current
         * rows should belong to the same week.
         */
        NflInjurySnapshot firstSnapshot = snapshots.get(0);

        Integer seasonYear = firstSnapshot.getSeasonYear();

        String seasonType = firstSnapshot.getSeasonType();

        Integer weekNumber = firstSnapshot.getWeekNumber();

        if (seasonYear == null ||
                seasonType == null ||
                weekNumber == null) {
            return List.of();
        }

        /*
         * Load the entire week's practice
         * history in one database query.
         */
        List<NflInjuryPracticeReport> weeklyPracticeReports = practiceReportRepository
                .findByExternalProviderTeamIdAndSeasonYearAndSeasonTypeAndWeekNumberOrderByPlayerNameAscReportDateAsc(
                        providerTeamId,
                        seasonYear,
                        seasonType,
                        weekNumber);

        /*
         * Group each player's Wednesday /
         * Thursday / Friday/etc. reports.
         */
        Map<String, List<NflInjuryPracticeReport>> reportsByPlayer = weeklyPracticeReports
                .stream()
                .collect(
                        Collectors.groupingBy(
                                NflInjuryPracticeReport::getPlayerProviderId));

        return snapshots
                .stream()
                .map(
                        snapshot -> {

                            List<NflInjuryPracticeReport> playerReports = reportsByPlayer
                                    .getOrDefault(
                                            snapshot
                                                    .getPlayerProviderId(),
                                            List.of());

                            String position = getLatestPosition(
                                    playerReports);

                            List<NflInjuryPracticeDayResponse> practiceDays = playerReports
                                    .stream()
                                    .map(
                                            report -> new NflInjuryPracticeDayResponse(
                                                    report.getReportDate(),
                                                    report.getPracticeStatus()))
                                    .toList();

                            return new NflInjurySnapshotResponse(
                                    snapshot.getPlayerName(),
                                    snapshot.getPlayerProviderId(),
                                    nflPlayerImageService
                                            .getHeadshotUrl(
                                                    snapshot.getPlayerProviderId()),
                                    position,
                                    snapshot.getSeasonYear(),
                                    snapshot.getSeasonType(),
                                    snapshot.getWeekNumber(),
                                    snapshot.getInjury(),
                                    snapshot.getSecondaryInjury(),
                                    snapshot.getGameStatus(),
                                    snapshot.getStatusDate(),
                                    snapshot.getEstimatedReturnDate(),
                                    practiceDays,
                                    snapshot.getUpdatedAt());
                        })
                .toList();
    }

    private String getLatestPosition(
            List<NflInjuryPracticeReport> reports) {

        /*
         * Repository results are ordered by
         * report date, so walking through them
         * leaves us with the latest non-empty
         * position.
         */
        String latestPosition = "";

        for (NflInjuryPracticeReport report : reports) {
            if (report.getPosition() != null &&
                    !report.getPosition()
                            .isBlank()) {
                latestPosition = report.getPosition();
            }
        }

        return latestPosition;
    }
}