package sportsalerts;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "roster_events", uniqueConstraints = {
        @UniqueConstraint(columnNames = "dedupe_key")
})
public class RosterEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String league;

    @Column(name = "external_team_id")
    private Long externalTeamId;

    @Column(name = "team_name")
    private String teamName;

    @Column(name = "source_transaction_id")
    private Long sourceTransactionId;

    @Column(name = "player_id")
    private Long playerId;

    @Column(name = "player_name")
    private String playerName;

    @Column(name = "event_type")
    private String eventType;

    @Column(name = "event_date")
    private LocalDate eventDate;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "dedupe_key", nullable = false, length = 64)
    private String dedupeKey;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    public RosterEvent() {
    }

    public RosterEvent(
            String league,
            Long externalTeamId,
            String teamName,
            Long sourceTransactionId,
            Long playerId,
            String playerName,
            String eventType,
            LocalDate eventDate,
            String description,
            String dedupeKey) {
        this.league = league;
        this.externalTeamId = externalTeamId;
        this.teamName = teamName;
        this.sourceTransactionId = sourceTransactionId;
        this.playerId = playerId;
        this.playerName = playerName;
        this.eventType = eventType;
        this.eventDate = eventDate;
        this.description = description;
        this.dedupeKey = dedupeKey;
        this.createdAt = LocalDateTime.now(
                java.time.ZoneOffset.UTC);
    }

    public Long getId() {
        return id;
    }

    public String getLeague() {
        return league;
    }

    public Long getExternalTeamId() {
        return externalTeamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public Long getSourceTransactionId() {
        return sourceTransactionId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getPlayerName() {
        return playerName;
    }

    public String getEventType() {
        return eventType;
    }

    public LocalDate getEventDate() {
        return eventDate;
    }

    public String getDescription() {
        return description;
    }

    public String getDedupeKey() {
        return dedupeKey;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}