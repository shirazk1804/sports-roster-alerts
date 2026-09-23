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
@RequestMapping(
    "/api/followed-teams"
)
public class NflInjurySnapshotController {

    private final FollowedTeamRepository
        followedTeamRepository;

    private final TeamRepository
        teamRepository;

    private final NflInjurySnapshotRepository
        snapshotRepository;

    private final AppUserService
        appUserService;

    public NflInjurySnapshotController(
        FollowedTeamRepository followedTeamRepository,
        TeamRepository teamRepository,
        NflInjurySnapshotRepository snapshotRepository,
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

    @GetMapping(
        "/{followedTeamId}/injuries"
    )
    public List<NflInjurySnapshotResponse>
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
         * Do not expose another user's
         * followed-team data.
         */
        if (
            followedTeam.getAppUser()
                == null ||
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
            !"NFL".equals(
                followedTeam.getLeague()
            )
        ) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Current injury reports are only available for NFL teams"
            );
        }

        Team team =
            teamRepository
                .findByLeagueAndName(
                    "NFL",
                    followedTeam.getName()
                )
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "NFL team not found"
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
                "NFL provider team ID not found"
            );
        }

        return snapshotRepository
            .findByExternalProviderTeamIdOrderByPlayerNameAsc(
                providerTeamId
            )
            .stream()
            .map(
                snapshot ->
                    new NflInjurySnapshotResponse(
                        snapshot.getPlayerName(),
                        snapshot.getInjury(),
                        snapshot.getSecondaryInjury(),
                        snapshot.getGameStatus(),
                        snapshot.getPracticeStatus(),
                        snapshot.getStatusDate(),
                        snapshot.getEstimatedReturnDate(),
                        snapshot.getUpdatedAt()
                    )
            )
            .toList();
    }
}