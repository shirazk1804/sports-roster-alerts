package sportsalerts;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/followed-teams")
public class NbaInjurySnapshotController {

    private final FollowedTeamRepository
        followedTeamRepository;

    private final TeamRepository
        teamRepository;

    private final NbaInjurySnapshotRepository
        snapshotRepository;

    private final AppUserService
        appUserService;

    public NbaInjurySnapshotController(
        FollowedTeamRepository followedTeamRepository,
        TeamRepository teamRepository,
        NbaInjurySnapshotRepository snapshotRepository,
        AppUserService appUserService
    ) {
        this.followedTeamRepository =
            followedTeamRepository;

        this.teamRepository =
            teamRepository;

        this.snapshotRepository =
            snapshotRepository;

        this.appUserService =
            appUserService;
    }

    @GetMapping("/{followedTeamId}/nba-injuries")
    public List<NbaInjurySnapshotResponse>
        getCurrentInjuries(

        @PathVariable
        Long followedTeamId,

        @RequestHeader(
            value = "Authorization",
            required = false
        )
        String authorizationHeader
    ) {

        AppUser user =
            appUserService
                .requireAuthenticatedUser(
                    authorizationHeader
                );

        FollowedTeam followedTeam =
            followedTeamRepository
                .findById(
                    followedTeamId
                )
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND
                        )
                );

        /*
         * Never expose another user's
         * followed-team information.
         */
        if (
            followedTeam.getAppUser() == null ||
            !followedTeam
                .getAppUser()
                .getId()
                .equals(
                    user.getId()
                )
        ) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND
            );
        }

        if (
            !"NBA".equals(
                followedTeam.getLeague()
            )
        ) {

            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "NBA injury reports are only available for NBA teams"
            );
        }

        Team team =
            teamRepository
                .findByLeagueAndName(
                    "NBA",
                    followedTeam.getName()
                )
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "NBA team not found"
                        )
                );

        String providerTeamId =
            team.getExternalProviderId();

        if (
            providerTeamId == null ||
            providerTeamId.isBlank()
        ) {

            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "NBA provider team ID not found"
            );
        }

        return snapshotRepository
            .findByExternalProviderTeamIdOrderByPlayerNameAsc(
                providerTeamId
            )
            .stream()
            .map(
                snapshot ->
                    new NbaInjurySnapshotResponse(

                        snapshot
                            .getPlayerName(),

                        snapshot
                            .getPlayerProviderId(),

                        snapshot
                            .getPosition(),

                        snapshot
                            .getInjury(),

                        snapshot
                            .getStatus(),

                        snapshot
                            .getComment(),

                        snapshot
                            .getStartDate(),

                        snapshot
                            .getProviderUpdateDate(),

                        snapshot
                            .getUpdatedAt()
                    )
            )
            .toList();
    }
}