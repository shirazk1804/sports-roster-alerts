package sportsalerts;

import java.time.LocalDate;
import java.util.List;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/followed-teams")
public class FollowedTeamController {

    private final FollowedTeamRepository followedTeamRepository;
    private final AlertPreferenceRepository alertPreferenceRepository;
    private final TeamRepository teamRepository;
    private final MlbTransactionService mlbTransactionService;
    private final RosterEventService rosterEventService;

    public FollowedTeamController(
        FollowedTeamRepository followedTeamRepository,
        AlertPreferenceRepository alertPreferenceRepository,
        TeamRepository teamRepository,
        MlbTransactionService mlbTransactionService,
        RosterEventService rosterEventService
    ) {
        this.followedTeamRepository = followedTeamRepository;
        this.alertPreferenceRepository = alertPreferenceRepository;
        this.teamRepository = teamRepository;
        this.mlbTransactionService = mlbTransactionService;
        this.rosterEventService = rosterEventService;
    }

    @GetMapping
    public List<FollowedTeam> getFollowedTeams() {
        return followedTeamRepository.findAll();
    }

    @PostMapping
    public FollowedTeam followTeam(
        @RequestBody FollowedTeam followedTeam
    ) {
        boolean alreadyFollowing =
            followedTeamRepository.existsByLeagueAndName(
                followedTeam.getLeague(),
                followedTeam.getName()
            );

        if (alreadyFollowing) {
            return followedTeamRepository
                .findAll()
                .stream()
                .filter(team ->
                    team.getLeague().equals(
                        followedTeam.getLeague()
                    )
                    &&
                    team.getName().equals(
                        followedTeam.getName()
                    )
                )
                .findFirst()
                .orElse(followedTeam);
        }

        FollowedTeam savedTeam =
            followedTeamRepository.save(
                followedTeam
            );

        if ("MLB".equals(savedTeam.getLeague())) {
            backfillMlbTransactions(savedTeam);
        }

        return savedTeam;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void unfollowTeam(
        @PathVariable Long id
    ) {
        alertPreferenceRepository
            .deleteByFollowedTeamId(id);

        followedTeamRepository
            .deleteById(id);
    }

    private void backfillMlbTransactions(
        FollowedTeam followedTeam
    ) {
        Team team =
            teamRepository
                .findByLeagueAndName(
                    "MLB",
                    followedTeam.getName()
                )
                .orElse(null);

        if (
            team == null ||
            team.getExternalTeamId() == null
        ) {
            System.err.println(
                "Could not backfill MLB transactions. "
                + "No external team ID found for "
                + followedTeam.getName()
            );

            return;
        }

        LocalDate endDate =
            LocalDate.now();

        LocalDate startDate =
            endDate.minusDays(7);

        try {
            List<MlbTransactionEvent> events =
                mlbTransactionService
                    .getNormalizedTransactions(
                        team.getExternalTeamId(),
                        startDate.toString(),
                        endDate.toString()
                    );

            List<RosterEvent> savedEvents =
                rosterEventService
                    .saveMlbEvents(
                        team.getExternalTeamId(),
                        events
                    );

            System.out.println(
                "MLB backfill complete for "
                + followedTeam.getName()
                + ". Saved "
                + savedEvents.size()
                + " recent events."
            );

        } catch (Exception exception) {
            System.err.println(
                "MLB backfill failed for "
                + followedTeam.getName()
                + ": "
                + exception.getMessage()
            );
        }
    }
}