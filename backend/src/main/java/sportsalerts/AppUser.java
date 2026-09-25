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
@Table(name = "app_users", uniqueConstraints = {
        @UniqueConstraint(columnNames = "installation_id"),
        @UniqueConstraint(columnNames = "auth_token_hash")
})
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "installation_id", nullable = false, unique = true, length = 100)
    private String installationId;

    @Column(name = "auth_token_hash", unique = true, length = 64)
    private String authTokenHash;

    @Column(name = "auth_token_expires_at")
    private LocalDateTime authTokenExpiresAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public AppUser() {
    }

    public AppUser(
            String installationId) {
        this.installationId = installationId;

        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public String getInstallationId() {
        return installationId;
    }

    public void setAuthTokenHash(
            String authTokenHash) {
        this.authTokenHash = authTokenHash;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getAuthTokenExpiresAt() {
        return authTokenExpiresAt;
    }

    public void setAuthTokenExpiresAt(
            LocalDateTime authTokenExpiresAt) {

        this.authTokenExpiresAt = authTokenExpiresAt;
    }

    String getAuthTokenHash() {
        return authTokenHash;
    }
}