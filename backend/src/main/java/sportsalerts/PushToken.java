package sportsalerts;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
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
        length = 255
    )
    private String expoPushToken;

    private String platform;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public PushToken() {
    }

    public PushToken(
        String expoPushToken,
        String platform
    ) {
        this.expoPushToken =
            expoPushToken;

        this.platform =
            platform;

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
        this.platform = platform;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(
        LocalDateTime updatedAt
    ) {
        this.updatedAt = updatedAt;
    }
}