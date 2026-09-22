package sportsalerts;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
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
    name = "push_tokens",
    uniqueConstraints = {
        @UniqueConstraint(
            columnNames = "expo_push_token"
        )
    }
)
public class PushToken {

    @Id
    @GeneratedValue(
        strategy = GenerationType.IDENTITY
    )
    private Long id;

    @Column(
        name = "expo_push_token",
        nullable = false,
        unique = true,
        length = 512
    )
    private String expoPushToken;

    @Column(
        nullable = false
    )
    private String platform;

    @Column(
        name = "updated_at",
        nullable = false
    )
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
        name = "app_user_id"
    )
    private AppUser appUser;

    public PushToken() {
    }

    public PushToken(
        String expoPushToken,
        String platform,
        AppUser appUser
    ) {
        this.expoPushToken =
            expoPushToken;

        this.platform =
            platform;

        this.appUser =
            appUser;

        this.updatedAt =
            LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getExpoPushToken() {
        return expoPushToken;
    }

    public void setExpoPushToken(
        String expoPushToken
    ) {
        this.expoPushToken =
            expoPushToken;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(
        String platform
    ) {
        this.platform =
            platform;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
        LocalDateTime updatedAt
    ) {
        this.updatedAt =
            updatedAt;
    }

    public AppUser getAppUser() {
        return appUser;
    }

    public void setAppUser(
        AppUser appUser
    ) {
        this.appUser =
            appUser;
    }
}