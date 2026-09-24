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
    name = "nfl_injury_practice_reports",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {
                "external_provider_team_id",
                "player_provider_id",
                "season_year",
                "season_type",
                "week_number",
                "report_date"
            }
        )
    }
)
public class NflInjuryPracticeReport {

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
        name = "position",
        length = 20
    )
    private String position;

    @Column(
        name = "season_year",
        nullable = false
    )
    private Integer seasonYear;

    @Column(
        name = "season_type",
        nullable = false,
        length = 20
    )
    private String seasonType;

    @Column(
        name = "week_number",
        nullable = false
    )
    private Integer weekNumber;

    @Column(
        name = "report_date",
        nullable = false
    )
    private LocalDate reportDate;

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
        name = "updated_at"
    )
    private LocalDateTime updatedAt;

    public NflInjuryPracticeReport() {
    }

    public NflInjuryPracticeReport(
        String externalProviderTeamId,
        String teamName,
        String playerProviderId,
        String playerName,
        String position,
        int seasonYear,
        String seasonType,
        int weekNumber,
        LocalDate reportDate,
        String injury,
        String secondaryInjury,
        String gameStatus,
        String practiceStatus,
        String statusDate
    ) {
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

        this.seasonYear =
            seasonYear;

        this.seasonType =
            seasonType;

        this.weekNumber =
            weekNumber;

        this.reportDate =
            reportDate;

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

    public String getPosition() {
        return position;
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

    public LocalDate getReportDate() {
        return reportDate;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void update(
        String teamName,
        String playerName,
        String position,
        String injury,
        String secondaryInjury,
        String gameStatus,
        String practiceStatus,
        String statusDate
    ) {
        this.teamName =
            teamName;

        this.playerName =
            playerName;

        this.position =
            position;

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

        this.updatedAt =
            LocalDateTime.now(
                ZoneOffset.UTC
            );
    }
}