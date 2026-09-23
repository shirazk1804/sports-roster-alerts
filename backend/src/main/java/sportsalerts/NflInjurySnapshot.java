package sportsalerts;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "nfl_injury_snapshots",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {
                "external_provider_team_id",
                "player_provider_id"
            }
        )
    }
)
public class NflInjurySnapshot {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
        name = "external_provider_team_id",
        nullable = false,
        length = 100
    )
    private String externalProviderTeamId;

    @Column(
        name = "team_name",
        nullable = false
    )
    private String teamName;

    @Column(
        name = "player_provider_id",
        nullable = false,
        length = 100
    )
    private String playerProviderId;

    @Column(
        name = "player_name",
        nullable = false
    )
    private String playerName;

    @Column(
        name = "season_year"
    )
    private Integer seasonYear;

    @Column(
        name = "season_type",
        length = 20
    )
    private String seasonType;

    @Column(
        name = "week_number"
    )
    private Integer weekNumber;

    @Column(
        name = "injury"
    )
    private String injury;

    @Column(
        name = "secondary_injury"
    )
    private String secondaryInjury;

    @Column(
        name = "game_status"
    )
    private String gameStatus;

    @Column(
        name = "practice_status"
    )
    private String practiceStatus;

    @Column(
        name = "status_date"
    )
    private String statusDate;

    @Column(
        name = "estimated_return_date"
    )
    private String estimatedReturnDate;

    @Column(
        name = "state_hash",
        nullable = false,
        length = 64
    )
    private String stateHash;

    @Column(
        name = "updated_at"
    )
    private LocalDateTime updatedAt;

    public NflInjurySnapshot() {
    }

    public NflInjurySnapshot(
        String externalProviderTeamId,
        String teamName,
        String playerProviderId,
        String playerName,
        int seasonYear,
        String seasonType,
        int weekNumber,
        String injury,
        String secondaryInjury,
        String gameStatus,
        String practiceStatus,
        String statusDate,
        String estimatedReturnDate,
        String stateHash
    ) {
        this.externalProviderTeamId =
            externalProviderTeamId;

        this.teamName =
            teamName;

        this.playerProviderId =
            playerProviderId;

        this.playerName =
            playerName;

        this.seasonYear =
            seasonYear;

        this.seasonType =
            seasonType;

        this.weekNumber =
            weekNumber;

        this.injury =
            injury;

        this.secondaryInjury =
            secondaryInjury;

        this.gameStatus =
            gameStatus;

        this.practiceStatus =
            practiceStatus;

        this.statusDate =
            statusDate;

        this.estimatedReturnDate =
            estimatedReturnDate;

        this.stateHash =
            stateHash;

        this.updatedAt =
            LocalDateTime.now(
                ZoneOffset.UTC
            );
    }

    public Long getId() {
        return id;
    }

    public String getExternalProviderTeamId() {
        return externalProviderTeamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getPlayerProviderId() {
        return playerProviderId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public Integer getSeasonYear() {
        return seasonYear;
    }

    public String getSeasonType() {
        return seasonType;
    }

    public Integer getWeekNumber() {
        return weekNumber;
    }

    public String getInjury() {
        return injury;
    }

    public String getSecondaryInjury() {
        return secondaryInjury;
    }

    public String getGameStatus() {
        return gameStatus;
    }

    public String getPracticeStatus() {
        return practiceStatus;
    }

    public String getStatusDate() {
        return statusDate;
    }

    public String getEstimatedReturnDate() {
        return estimatedReturnDate;
    }

    public String getStateHash() {
        return stateHash;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
        String teamName,
        String playerName,
        String injury,
        String secondaryInjury,
        String gameStatus,
        String practiceStatus,
        String statusDate,
        String estimatedReturnDate,
        String stateHash
    ) {
        this.teamName =
            teamName;

        this.playerName =
            playerName;

        this.injury =
            injury;

        this.secondaryInjury =
            secondaryInjury;

        this.gameStatus =
            gameStatus;

        this.practiceStatus =
            practiceStatus;

        this.statusDate =
            statusDate;

        this.estimatedReturnDate =
            estimatedReturnDate;

        this.stateHash =
            stateHash;

        this.updatedAt =
            LocalDateTime.now(
                ZoneOffset.UTC
            );
    }
}