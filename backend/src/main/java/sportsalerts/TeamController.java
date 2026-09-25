package sportsalerts;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/teams")
public class TeamController {

    public record TeamResponse(
            Long id,
            String league,
            String name,
            String abbreviation) {
    }

    private final TeamRepository teamRepository;

    public TeamController(
            TeamRepository teamRepository) {

        this.teamRepository =
                teamRepository;
    }

    @GetMapping
    public List<TeamResponse> getTeams() {

        return teamRepository
                .findAll()
                .stream()
                .map(team ->
                        new TeamResponse(
                                team.getId(),
                                team.getLeague(),
                                team.getName(),
                                team.getAbbreviation()))
                .toList();
    }
}