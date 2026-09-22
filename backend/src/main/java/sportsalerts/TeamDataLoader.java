package sportsalerts;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class TeamDataLoader
    implements CommandLineRunner {

    private final TeamRepository
        teamRepository;

    public TeamDataLoader(
        TeamRepository teamRepository
    ) {
        this.teamRepository =
            teamRepository;
    }

    @Override
    public void run(String... args) {

        // Existing NFL / NBA prototype teams

        upsertTeam(
            "NFL",
            "Los Angeles Rams",
            "LAR",
            null
        );

        upsertTeam(
            "NBA",
            "Los Angeles Lakers",
            "LAL",
            null
        );

        // MLB teams

        upsertTeam("MLB", "Los Angeles Angels", "LAA", 108L);
        upsertTeam("MLB", "Arizona Diamondbacks", "ARI", 109L);
        upsertTeam("MLB", "Baltimore Orioles", "BAL", 110L);
        upsertTeam("MLB", "Boston Red Sox", "BOS", 111L);
        upsertTeam("MLB", "Chicago Cubs", "CHC", 112L);
        upsertTeam("MLB", "Cincinnati Reds", "CIN", 113L);
        upsertTeam("MLB", "Cleveland Guardians", "CLE", 114L);
        upsertTeam("MLB", "Colorado Rockies", "COL", 115L);
        upsertTeam("MLB", "Detroit Tigers", "DET", 116L);
        upsertTeam("MLB", "Houston Astros", "HOU", 117L);
        upsertTeam("MLB", "Kansas City Royals", "KC", 118L);
        upsertTeam("MLB", "Los Angeles Dodgers", "LAD", 119L);
        upsertTeam("MLB", "Washington Nationals", "WSH", 120L);
        upsertTeam("MLB", "New York Mets", "NYM", 121L);
        upsertTeam("MLB", "Athletics", "ATH", 133L);
        upsertTeam("MLB", "Pittsburgh Pirates", "PIT", 134L);
        upsertTeam("MLB", "San Diego Padres", "SD", 135L);
        upsertTeam("MLB", "Seattle Mariners", "SEA", 136L);
        upsertTeam("MLB", "San Francisco Giants", "SF", 137L);
        upsertTeam("MLB", "St. Louis Cardinals", "STL", 138L);
        upsertTeam("MLB", "Tampa Bay Rays", "TB", 139L);
        upsertTeam("MLB", "Texas Rangers", "TEX", 140L);
        upsertTeam("MLB", "Toronto Blue Jays", "TOR", 141L);
        upsertTeam("MLB", "Minnesota Twins", "MIN", 142L);
        upsertTeam("MLB", "Philadelphia Phillies", "PHI", 143L);
        upsertTeam("MLB", "Atlanta Braves", "ATL", 144L);
        upsertTeam("MLB", "Chicago White Sox", "CWS", 145L);
        upsertTeam("MLB", "Miami Marlins", "MIA", 146L);
        upsertTeam("MLB", "New York Yankees", "NYY", 147L);
        upsertTeam("MLB", "Milwaukee Brewers", "MIL", 158L);
    }

    private void upsertTeam(
        String league,
        String name,
        String abbreviation,
        Long externalTeamId
    ) {
        Team team =
            teamRepository
                .findByLeagueAndName(
                    league,
                    name
                )
                .orElse(
                    new Team(
                        league,
                        name,
                        abbreviation,
                        externalTeamId
                    )
                );

        team.setAbbreviation(
            abbreviation
        );

        team.setExternalTeamId(
            externalTeamId
        );

        teamRepository.save(team);
    }
}