package sportsalerts;

import java.time.LocalDate;
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
    name = "roster_events",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = "dedupe_key"
        )
    }
)
public class RosterEvent {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    private String league;

    /*
     * MLB numeric team ID.
     *
     * Example:
     * Dodgers = 119
     */
    @Column(
        name = "external_team_id"
    )
    private Long externalTeamId;

    /*
     * NFL/NBA provider GUID.
     */
    @Column(
        name = "external_provider_team_id",
        length = 100
    )
    private String externalProviderTeamId;

    @Column(
        name = "team_name"
    )
    private String teamName;

    /*
     * MLB transaction ID.
     */
    @Column(
        name = "source_transaction_id"
    )
    private Long sourceTransactionId;

    /*
     * NFL/NBA provider transaction/event ID.
     */
    @Column(
        name = "source_provider_event_id",
        length = 100
    )
    private String sourceProviderEventId;

    /*
     * MLB numeric player ID.
     */
    @Column(
        name = "player_id"
    )
    private Long playerId;

    /*
     * NFL/NBA provider player GUID.
     */
    @Column(
        name = "player_provider_id",
        length = 100
    )
    private String playerProviderId;

    @Column(
        name = "player_name"
    )
    private String playerName;

    @Column(
        name = "event_type"
    )
    private String eventType;

    @Column(
        name = "event_date"
    )
    private LocalDate eventDate;

    @Column(
        columnDefinition = "TEXT"
    )
    private String description;

    @Column(
        name = "dedupe_key",
        nullable = false,
        length = 64
    )
    private String dedupeKey;

    @Column(
        name = "created_at"
    )
    private LocalDateTime createdAt;

    public RosterEvent() {
    }

    /*
     * Existing MLB constructor.
     */
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
        String dedupeKey
    ) {
        this.league =
            league;

        this.externalTeamId =
            externalTeamId;

        this.teamName =
            teamName;

        this.sourceTransactionId =
            sourceTransactionId;

        this.playerId =
            playerId;

        this.playerName =
            playerName;

        this.eventType =
            eventType;

        this.eventDate =
            eventDate;

        this.description =
            description;

        this.dedupeKey =
            dedupeKey;

        this.createdAt =
            LocalDateTime.now(
                ZoneOffset.UTC
            );
    }

    /*
     * NFL/NBA provider constructor.
     */
    public RosterEvent(
        String league,
        String externalProviderTeamId,
        String teamName,
        String sourceProviderEventId,
        String playerProviderId,
        String playerName,
        String eventType,
        LocalDate eventDate,
        String description,
        String dedupeKey
    ) {
        this.league =
            league;

        this.externalProviderTeamId =
            externalProviderTeamId;

        this.teamName =
            teamName;

        this.sourceProviderEventId =
            sourceProviderEventId;

        this.playerProviderId =
            playerProviderId;

        this.playerName =
            playerName;

        this.eventType =
            eventType;

        this.eventDate =
            eventDate;

        this.description =
            description;

        this.dedupeKey =
            dedupeKey;

        this.createdAt =
            LocalDateTime.now(
                ZoneOffset.UTC
            );
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

    public String getExternalProviderTeamId() {
        return externalProviderTeamId;
    }

    public String getTeamName() {
        return teamName;
    }

    public Long getSourceTransactionId() {
        return sourceTransactionId;
    }

    public String getSourceProviderEventId() {
        return sourceProviderEventId;
    }

    public Long getPlayerId() {
        return playerId;
    }

    public String getPlayerProviderId() {
        return playerProviderId;
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