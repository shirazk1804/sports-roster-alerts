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
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    private String league;

    private String name;

    private String abbreviation;

    /*
     * MLB Stats API uses numeric team IDs.
     *
     * Example:
     * Dodgers = 119
     */
    @Column(
        name = "external_team_id"
    )
    private Long externalTeamId;

    /*
     * Providers such as Sportradar use
     * string/GUID identifiers.
     *
     * NFL and NBA will use this field.
     */
    @Column(
        name = "external_provider_id",
        length = 100
    )
    private String externalProviderId;

    public Team() {
    }

    /*
     * Existing MLB constructor.
     *
     * Keeping this means our current
     * TeamDataLoader continues working.
     */
    public Team(
        String league,
        String name,
        String abbreviation,
        Long externalTeamId
    ) {
        this.league = league;
        this.name = name;
        this.abbreviation = abbreviation;
        this.externalTeamId =
            externalTeamId;
    }

    /*
     * Constructor for NFL/NBA teams.
     */
    public Team(
        String league,
        String name,
        String abbreviation,
        String externalProviderId
    ) {
        this.league = league;
        this.name = name;
        this.abbreviation = abbreviation;
        this.externalProviderId =
            externalProviderId;
    }

    public Long getId() {
        return id;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(
        String league
    ) {
        this.league = league;
    }

    public String getName() {
        return name;
    }

    public void setName(
        String name
    ) {
        this.name = name;
    }

    public String getAbbreviation() {
        return abbreviation;
    }

    public void setAbbreviation(
        String abbreviation
    ) {
        this.abbreviation =
            abbreviation;
    }

    public Long getExternalTeamId() {
        return externalTeamId;
    }

    public void setExternalTeamId(
        Long externalTeamId
    ) {
        this.externalTeamId =
            externalTeamId;
    }

    public String getExternalProviderId() {
        return externalProviderId;
    }

    public void setExternalProviderId(
        String externalProviderId
    ) {
        this.externalProviderId =
            externalProviderId;
    }
}