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
        name = "nba_player_headshots",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {
                                "nba_player_id",
                                "team_name"
                        })
        })
public class NbaPlayerHeadshot {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "nba_player_id",
            nullable = false,
            length = 50)
    private String nbaPlayerId;

    @Column(
            name = "player_name",
            nullable = false)
    private String playerName;

    @Column(
            name = "team_name",
            nullable = false)
    private String teamName;

    @Column(
            name = "headshot_url",
            nullable = false,
            columnDefinition = "TEXT")
    private String headshotUrl;

    @Column(
            name = "updated_at",
            nullable = false)
    private LocalDateTime updatedAt;

    public NbaPlayerHeadshot() {
    }

    public NbaPlayerHeadshot(
            String nbaPlayerId,
            String playerName,
            String teamName,
            String headshotUrl) {

        this.nbaPlayerId =
                nbaPlayerId;

        this.playerName =
                playerName;

        this.teamName =
                teamName;

        this.headshotUrl =
                headshotUrl;

        this.updatedAt =
                LocalDateTime.now(
                        ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getNbaPlayerId() {
        return nbaPlayerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getTeamName() {
        return teamName;
    }

    public String getHeadshotUrl() {
        return headshotUrl;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
            String playerName,
            String teamName,
            String headshotUrl) {

        this.playerName =
                playerName;

        this.teamName =
                teamName;

        this.headshotUrl =
                headshotUrl;

        this.updatedAt =
                LocalDateTime.now(
                        ZoneOffset.UTC);
    }
}