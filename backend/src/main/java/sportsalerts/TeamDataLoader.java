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

        // NFL teams

        upsertProviderTeam(
            "NFL",
            "Arizona Cardinals",
            "ARI"
        );

        upsertProviderTeam(
            "NFL",
            "Atlanta Falcons",
            "ATL"
        );

        upsertProviderTeam(
            "NFL",
            "Baltimore Ravens",
            "BAL"
        );

        upsertProviderTeam(
            "NFL",
            "Buffalo Bills",
            "BUF"
        );

        upsertProviderTeam(
            "NFL",
            "Carolina Panthers",
            "CAR"
        );

        upsertProviderTeam(
            "NFL",
            "Chicago Bears",
            "CHI"
        );

        upsertProviderTeam(
            "NFL",
            "Cincinnati Bengals",
            "CIN"
        );

        upsertProviderTeam(
            "NFL",
            "Cleveland Browns",
            "CLE"
        );

        upsertProviderTeam(
            "NFL",
            "Dallas Cowboys",
            "DAL"
        );

        upsertProviderTeam(
            "NFL",
            "Denver Broncos",
            "DEN"
        );

        upsertProviderTeam(
            "NFL",
            "Detroit Lions",
            "DET"
        );

        upsertProviderTeam(
            "NFL",
            "Green Bay Packers",
            "GB"
        );

        upsertProviderTeam(
            "NFL",
            "Houston Texans",
            "HOU"
        );

        upsertProviderTeam(
            "NFL",
            "Indianapolis Colts",
            "IND"
        );

        upsertProviderTeam(
            "NFL",
            "Jacksonville Jaguars",
            "JAC"
        );

        upsertProviderTeam(
            "NFL",
            "Kansas City Chiefs",
            "KC"
        );

        upsertProviderTeam(
            "NFL",
            "Las Vegas Raiders",
            "LV"
        );

        upsertProviderTeam(
            "NFL",
            "Los Angeles Chargers",
            "LAC"
        );

        upsertProviderTeam(
            "NFL",
            "Los Angeles Rams",
            "LAR"
        );

        upsertProviderTeam(
            "NFL",
            "Miami Dolphins",
            "MIA"
        );

        upsertProviderTeam(
            "NFL",
            "Minnesota Vikings",
            "MIN"
        );

        upsertProviderTeam(
            "NFL",
            "New England Patriots",
            "NE"
        );

        upsertProviderTeam(
            "NFL",
            "New Orleans Saints",
            "NO"
        );

        upsertProviderTeam(
            "NFL",
            "New York Giants",
            "NYG"
        );

        upsertProviderTeam(
            "NFL",
            "New York Jets",
            "NYJ"
        );

        upsertProviderTeam(
            "NFL",
            "Philadelphia Eagles",
            "PHI"
        );

        upsertProviderTeam(
            "NFL",
            "Pittsburgh Steelers",
            "PIT"
        );

        upsertProviderTeam(
            "NFL",
            "San Francisco 49ers",
            "SF"
        );

        upsertProviderTeam(
            "NFL",
            "Seattle Seahawks",
            "SEA"
        );

        upsertProviderTeam(
            "NFL",
            "Tampa Bay Buccaneers",
            "TB"
        );

        upsertProviderTeam(
            "NFL",
            "Tennessee Titans",
            "TEN"
        );

        upsertProviderTeam(
            "NFL",
            "Washington Commanders",
            "WAS"
        );

        // NBA prototype.
        // We'll add the full NBA catalog
        // when we start the NBA integration.

        upsertProviderTeam(
            "NBA",
            "Los Angeles Lakers",
            "LAL"
        );

        // MLB teams

        upsertMlbTeam(
            "Los Angeles Angels",
            "LAA",
            108L
        );

        upsertMlbTeam(
            "Arizona Diamondbacks",
            "ARI",
            109L
        );

        upsertMlbTeam(
            "Baltimore Orioles",
            "BAL",
            110L
        );

        upsertMlbTeam(
            "Boston Red Sox",
            "BOS",
            111L
        );

        upsertMlbTeam(
            "Chicago Cubs",
            "CHC",
            112L
        );

        upsertMlbTeam(
            "Cincinnati Reds",
            "CIN",
            113L
        );

        upsertMlbTeam(
            "Cleveland Guardians",
            "CLE",
            114L
        );

        upsertMlbTeam(
            "Colorado Rockies",
            "COL",
            115L
        );

        upsertMlbTeam(
            "Detroit Tigers",
            "DET",
            116L
        );

        upsertMlbTeam(
            "Houston Astros",
            "HOU",
            117L
        );

        upsertMlbTeam(
            "Kansas City Royals",
            "KC",
            118L
        );

        upsertMlbTeam(
            "Los Angeles Dodgers",
            "LAD",
            119L
        );

        upsertMlbTeam(
            "Washington Nationals",
            "WSH",
            120L
        );

        upsertMlbTeam(
            "New York Mets",
            "NYM",
            121L
        );

        upsertMlbTeam(
            "Athletics",
            "ATH",
            133L
        );

        upsertMlbTeam(
            "Pittsburgh Pirates",
            "PIT",
            134L
        );

        upsertMlbTeam(
            "San Diego Padres",
            "SD",
            135L
        );

        upsertMlbTeam(
            "Seattle Mariners",
            "SEA",
            136L
        );

        upsertMlbTeam(
            "San Francisco Giants",
            "SF",
            137L
        );

        upsertMlbTeam(
            "St. Louis Cardinals",
            "STL",
            138L
        );

        upsertMlbTeam(
            "Tampa Bay Rays",
            "TB",
            139L
        );

        upsertMlbTeam(
            "Texas Rangers",
            "TEX",
            140L
        );

        upsertMlbTeam(
            "Toronto Blue Jays",
            "TOR",
            141L
        );

        upsertMlbTeam(
            "Minnesota Twins",
            "MIN",
            142L
        );

        upsertMlbTeam(
            "Philadelphia Phillies",
            "PHI",
            143L
        );

        upsertMlbTeam(
            "Atlanta Braves",
            "ATL",
            144L
        );

        upsertMlbTeam(
            "Chicago White Sox",
            "CWS",
            145L
        );

        upsertMlbTeam(
            "Miami Marlins",
            "MIA",
            146L
        );

        upsertMlbTeam(
            "New York Yankees",
            "NYY",
            147L
        );

        upsertMlbTeam(
            "Milwaukee Brewers",
            "MIL",
            158L
        );
    }

    private void upsertMlbTeam(
        String name,
        String abbreviation,
        Long externalTeamId
    ) {
        Team team =
            teamRepository
                .findByLeagueAndName(
                    "MLB",
                    name
                )
                .orElse(
                    new Team(
                        "MLB",
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

        teamRepository.save(
            team
        );
    }

    private void upsertProviderTeam(
        String league,
        String name,
        String abbreviation
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
                        (String) null
                    )
                );

        team.setAbbreviation(
            abbreviation
        );

        /*
         * Do NOT set externalProviderId
         * here.
         *
         * Once Sportradar gives us the
         * team's GUID, we want it to stay
         * saved across application restarts.
         */

        teamRepository.save(
            team
        );
    }
}