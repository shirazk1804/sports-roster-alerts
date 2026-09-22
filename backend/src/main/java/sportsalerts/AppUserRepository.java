package sportsalerts;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AppUserRepository
    extends JpaRepository<AppUser, Long> {

    Optional<AppUser>
        findByInstallationId(
            String installationId
        );

    Optional<AppUser>
        findByAuthTokenHash(
            String authTokenHash
        );
}