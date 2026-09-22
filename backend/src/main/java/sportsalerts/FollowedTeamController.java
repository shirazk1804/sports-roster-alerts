package sportsalerts;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public FollowedTeamController(
        FollowedTeamRepository followedTeamRepository,
        AlertPreferenceRepository alertPreferenceRepository
    ) {
        this.followedTeamRepository = followedTeamRepository;
        this.alertPreferenceRepository = alertPreferenceRepository;
    }

    @GetMapping
    public List<FollowedTeam> getFollowedTeams() {
        return followedTeamRepository.findAll();
    }

    @PostMapping
    public ResponseEntity<?> followTeam(
        @RequestBody FollowedTeam team
    ) {
        boolean alreadyFollowing =
            followedTeamRepository.existsByLeagueAndName(
                team.getLeague(),
                team.getName()
            );

        if (alreadyFollowing) {
            return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(
                    Map.of(
                        "message",
                        "Team is already being followed"
                    )
                );
        }

        FollowedTeam savedTeam =
            followedTeamRepository.save(team);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(savedTeam);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<?> unfollowTeam(
        @PathVariable Long id
    ) {
        if (!followedTeamRepository.existsById(id)) {
            return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(
                    Map.of(
                        "message",
                        "Followed team not found"
                    )
                );
        }

        alertPreferenceRepository
            .deleteByFollowedTeamId(id);

        followedTeamRepository.deleteById(id);

        return ResponseEntity
            .noContent()
            .build();
    }
}