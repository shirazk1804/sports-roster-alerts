package sportsalerts;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/followed-teams")
public class FollowedTeamController {

    private final FollowedTeamRepository followedTeamRepository;

    private final AlertPreferenceRepository alertPreferenceRepository;

    private final TeamRepository teamRepository;

    private final MlbTransactionService mlbTransactionService;

    private final RosterEventService rosterEventService;

    private final AppUserService appUserService;

    private final NflFollowInitializationService nflFollowInitializationService;

    public FollowedTeamController(
            FollowedTeamRepository followedTeamRepository,
            AlertPreferenceRepository alertPreferenceRepository,
            TeamRepository teamRepository,
            MlbTransactionService mlbTransactionService,
            RosterEventService rosterEventService,
            AppUserService appUserService,
            NflFollowInitializationService nflFollowInitializationService) {
        this.followedTeamRepository = followedTeamRepository;

        this.alertPreferenceRepository = alertPreferenceRepository;

        this.teamRepository = teamRepository;

        this.mlbTransactionService = mlbTransactionService;

        this.rosterEventService = rosterEventService;

        this.appUserService = appUserService;

        this.nflFollowInitializationService = nflFollowInitializationService;
    }

    @GetMapping
    public List<FollowedTeam> getFollowedTeams(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AppUser user = appUserService
                .requireAuthenticatedUser(
                        authorizationHeader);

        return followedTeamRepository
                .findByAppUserId(
                        user.getId());
    }

    @PostMapping
    public FollowedTeam followTeam(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,

            @RequestBody FollowTeamRequest request) {

        AppUser user = appUserService
                .requireAuthenticatedUser(
                        authorizationHeader);

        if (request == null ||
                request.league() == null ||
                request.league().isBlank() ||
                request.name() == null ||
                request.name().isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "League and team name are required");
        }

        String league = request.league()
                .trim()
                .toUpperCase();

        String name = request.name()
                .trim();

        /*
         * Reject excessively large input before
         * doing any database/provider work.
         */
        if (league.length() > 10 ||
                name.length() > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid team information");
        }

        /*
         * Never trust the client to invent a
         * league/team combination.
         *
         * The team must already exist in our
         * server-controlled teams table.
         */
        Team validTeam = teamRepository
                .findByLeagueAndName(
                        league,
                        name)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "Invalid team"));

        boolean alreadyFollowing = followedTeamRepository
                .existsByAppUserIdAndLeagueAndName(
                        user.getId(),
                        validTeam.getLeague(),
                        validTeam.getName());

        if (alreadyFollowing) {

            return followedTeamRepository
                    .findByAppUserIdAndLeagueAndName(
                            user.getId(),
                            validTeam.getLeague(),
                            validTeam.getName())
                    .orElseThrow();
        }

        /*
         * Construct the entity ourselves rather
         * than allowing the request to populate
         * database-managed fields.
         */
        FollowedTeam followedTeam = new FollowedTeam(
                validTeam.getLeague(),
                validTeam.getName(),
                null,
                "All roster transactions");

        followedTeam.setAppUser(
                user);

        FollowedTeam savedTeam = followedTeamRepository.save(
                followedTeam);

        /*
         * MLB currently performs its
         * transaction backfill immediately.
         */
        if ("MLB".equals(
                savedTeam.getLeague())) {

            backfillMlbTransactions(
                    savedTeam);
        }

        /*
         * NFL initialization runs in the
         * background.
         */
        if ("NFL".equals(
                savedTeam.getLeague())) {

            nflFollowInitializationService
                    .initialize(
                            savedTeam.getName());
        }

        return savedTeam;
    }

    @DeleteMapping("/{id}")
    @Transactional
    public void unfollowTeam(
            @PathVariable Long id,

            @RequestHeader(value = "Authorization", required = false) String authorizationHeader) {
        AppUser user = appUserService
                .requireAuthenticatedUser(
                        authorizationHeader);

        FollowedTeam followedTeam = followedTeamRepository
                .findById(
                        id)
                .orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Followed team not found"));

        if (followedTeam.getAppUser() == null ||
                !followedTeam
                        .getAppUser()
                        .getId()
                        .equals(
                                user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Followed team not found");
        }

        alertPreferenceRepository
                .deleteByFollowedTeamId(
                        id);

        followedTeamRepository
                .delete(
                        followedTeam);
    }

    private void backfillMlbTransactions(
            FollowedTeam followedTeam) {
        Team team = teamRepository
                .findByLeagueAndName(
                        "MLB",
                        followedTeam.getName())
                .orElse(null);

        if (team == null ||
                team.getExternalTeamId() == null) {
            System.err.println(
                    "Could not backfill MLB transactions. "
                            + "No external team ID found for "
                            + followedTeam.getName());

            return;
        }

        LocalDate endDate = LocalDate.now();

        LocalDate startDate = endDate.minusDays(7);

        try {
            List<MlbTransactionEvent> events = mlbTransactionService
                    .getNormalizedTransactions(
                            team.getExternalTeamId(),
                            startDate.toString(),
                            endDate.toString());

            List<RosterEvent> savedEvents = rosterEventService
                    .saveMlbEvents(
                            team.getExternalTeamId(),
                            events);

            System.out.println(
                    "MLB backfill complete for "
                            + followedTeam.getName()
                            + ". Saved "
                            + savedEvents.size()
                            + " recent events.");

        } catch (Exception exception) {

            System.err.println(
                    "MLB backfill failed for "
                            + followedTeam.getName()
                            + ": "
                            + exception.getMessage());
        }
    }
}