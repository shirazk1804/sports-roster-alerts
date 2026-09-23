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