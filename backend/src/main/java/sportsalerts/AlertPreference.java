package sportsalerts;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "alert_preferences",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = {
                "followed_team_id",
                "alert_key"
            }
        )
    }
)
public class AlertPreference {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne
    @JoinColumn(
        name = "followed_team_id",
        nullable = false
    )
    private FollowedTeam followedTeam;

    @Column(name = "alert_key")
    private String alertKey;

    private boolean enabled;

    public AlertPreference() {
    }

    public AlertPreference(
        FollowedTeam followedTeam,
        String alertKey,
        boolean enabled
    ) {
        this.followedTeam = followedTeam;
        this.alertKey = alertKey;
        this.enabled = enabled;
    }

    public Long getId() {
        return id;
    }

    public FollowedTeam getFollowedTeam() {
        return followedTeam;
    }

    public void setFollowedTeam(
        FollowedTeam followedTeam
    ) {
        this.followedTeam = followedTeam;
    }

    public String getAlertKey() {
        return alertKey;
    }

    public void setAlertKey(
        String alertKey
    ) {
        this.alertKey = alertKey;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(
        boolean enabled
    ) {
        this.enabled = enabled;
    }
}