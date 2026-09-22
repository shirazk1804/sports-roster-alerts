package sportsalerts;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PushTokenRepository
    extends JpaRepository<PushToken, Long> {

    Optional<PushToken>
        findByExpoPushToken(
            String expoPushToken
        );

    List<PushToken>
        findByAppUserId(
            Long appUserId
        );
}