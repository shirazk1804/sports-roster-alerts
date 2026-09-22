package sportsalerts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "teams")
public class Team {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String league;

    private String name;

    private String abbreviation;

    @Column(name = "external_team_id")
    private Long externalTeamId;

    public Team() {
    }

    public Team(
        String league,
        String name,
        String abbreviation,
        Long externalTeamId
    ) {
        this.league = league;
        this.name = name;
        this.abbreviation = abbreviation;
        this.externalTeamId = externalTeamId;
    }

    public Long getId() {
        return id;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(
        String abbreviation
    ) {
        this.abbreviation = abbreviation;
    }

    public Long getExternalTeamId() {
        return externalTeamId;
    }

    public void setExternalTeamId(
        Long externalTeamId
    ) {
        this.externalTeamId = externalTeamId;
    }
}