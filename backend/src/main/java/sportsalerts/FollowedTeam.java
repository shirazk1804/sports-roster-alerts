package sportsalerts;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "followed_teams",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {
                "app_user_id",
                "league",
                "name"
            }
        )
    }
)
public class FollowedTeam {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    private String league;

    private String name;

    private String emoji;

    private String alertType;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(
        name = "app_user_id"
    )
    private AppUser appUser;

    public FollowedTeam() {
    }

    public FollowedTeam(
        String league,
        String name,
        String emoji,
        String alertType
    ) {
        this.league = league;
        this.name = name;
        this.emoji = emoji;
        this.alertType = alertType;
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

    public String getEmoji() {
        return emoji;
    }

    public void setEmoji(
        String emoji
    ) {
        this.emoji = emoji;
    }

    public String getAlertType() {
        return alertType;
    }

    public void setAlertType(
        String alertType
    ) {
        this.alertType = alertType;
    }

    public void setAppUser(
        AppUser appUser
    ) {
        this.appUser = appUser;
    }

    AppUser getAppUser() {
        return appUser;
    }
}