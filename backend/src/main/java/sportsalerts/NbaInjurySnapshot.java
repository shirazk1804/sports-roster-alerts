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
        name = "nba_injury_snapshots",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "external_provider_team_id",
                                "player_provider_id",
                                "injury_provider_id"
                        })
        })
public class NbaInjurySnapshot {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "external_provider_team_id",
            nullable = false,
            length = 100)
    private String externalProviderTeamId;

    @Column(
            name = "team_name",
            nullable = false)
    private String teamName;

    @Column(
            name = "player_provider_id",
            nullable = false,
            length = 100)
    private String playerProviderId;

    @Column(
            name = "player_name",
            nullable = false)
    private String playerName;

    @Column(
            name = "position",
            length = 20)
    private String position;

    @Column(
            name = "injury_provider_id",
            nullable = false,
            length = 100)
    private String injuryProviderId;

    @Column(
            name = "injury")
    private String injury;

    @Column(
            name = "status")
    private String status;

    @Column(
            name = "comment",
            columnDefinition = "TEXT")
    private String comment;

    @Column(
            name = "start_date")
    private String startDate;

    @Column(
            name = "provider_update_date")
    private String providerUpdateDate;

    @Column(
            name = "state_hash",
            nullable = false,
            length = 64)
    private String stateHash;

    @Column(
            name = "updated_at")
    private LocalDateTime updatedAt;

    public NbaInjurySnapshot() {
    }

    public NbaInjurySnapshot(
            String externalProviderTeamId,
            String teamName,
            String playerProviderId,
            String playerName,
            String position,
            String injuryProviderId,
            String injury,
            String status,
            String comment,
            String startDate,
            String providerUpdateDate,
            String stateHash) {

        this.externalProviderTeamId =
                externalProviderTeamId;

        this.teamName =
                teamName;

        this.playerProviderId =
                playerProviderId;

        this.playerName =
                playerName;

        this.position =
                position;

        this.injuryProviderId =
                injuryProviderId;

        this.injury =
                injury;

        this.status =
                status;

        this.comment =
                comment;

        this.startDate =
                startDate;

        this.providerUpdateDate =
                providerUpdateDate;

        this.stateHash =
                stateHash;

        this.updatedAt =
                LocalDateTime.now(
                        ZoneOffset.UTC);
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

    public String getPosition() {
        return position;
    }

    public String getInjuryProviderId() {
        return injuryProviderId;
    }

    public String getInjury() {
        return injury;
    }

    public String getStatus() {
        return status;
    }

    public String getComment() {
        return comment;
    }

    public String getStartDate() {
        return startDate;
    }

    public String getProviderUpdateDate() {
        return providerUpdateDate;
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
            String position,
            String injury,
            String status,
            String comment,
            String startDate,
            String providerUpdateDate,
            String stateHash) {

        this.teamName =
                teamName;

        this.playerName =
                playerName;

        this.position =
                position;

        this.injury =
                injury;

        this.status =
                status;

        this.comment =
                comment;

        this.startDate =
                startDate;

        this.providerUpdateDate =
                providerUpdateDate;

        this.stateHash =
                stateHash;

        this.updatedAt =
                LocalDateTime.now(
                        ZoneOffset.UTC);
    }
}