package sportsalerts;

import java.time.LocalDate;
import java.time.ZoneId;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/followed-teams")
public class MlbLineupController {

    private final FollowedTeamRepository
            followedTeamRepository;

    private final TeamRepository
            teamRepository;

    private final AppUserService
            appUserService;

    private final MlbLineupService
            mlbLineupService;

    public MlbLineupController(
            FollowedTeamRepository followedTeamRepository,
            TeamRepository teamRepository,
            AppUserService appUserService,
            MlbLineupService mlbLineupService) {

        this.followedTeamRepository =
                followedTeamRepository;

        this.teamRepository =
                teamRepository;

        this.appUserService =
                appUserService;

        this.mlbLineupService =
                mlbLineupService;
    }

    @GetMapping("/{followedTeamId}/lineup")
    public MlbLineupResponse getLineup(
            @PathVariable
            Long followedTeamId,

            @RequestHeader(
                    value = "Authorization",
                    required = false)
            String authorizationHeader) {

        AppUser user =
                appUserService
                        .requireAuthenticatedUser(
                                authorizationHeader);

        FollowedTeam followedTeam =
                followedTeamRepository
                        .findById(
                                followedTeamId)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Followed team not found"));

        /*
         * Prevent one user from requesting
         * another user's followed-team data.
         */
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

        if (!"MLB".equals(
                followedTeam.getLeague())) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Lineups are only available for MLB teams");
        }

        Team team =
                teamRepository
                        .findByLeagueAndName(
                                "MLB",
                                followedTeam.getName())
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "MLB team not found"));

        Long externalTeamId =
                team.getExternalTeamId();

        if (externalTeamId == null) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "MLB team ID not found");
        }

        /*
         * MLB schedules are organized around
         * the official U.S. game date.
         */
        LocalDate today =
                LocalDate.now(
                        ZoneId.of(
                                "America/New_York"));

        return mlbLineupService
                .getLineup(
                        externalTeamId,
                        today);
    }
}