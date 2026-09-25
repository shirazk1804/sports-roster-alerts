package sportsalerts;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApiRateLimitService {

    private static class AttemptWindow {

        private final Deque<Instant>
            attempts =
                new ArrayDeque<>();

        private volatile Instant
            lastAccess =
                Instant.now();
    }

    private final Map<String, AttemptWindow>
        windows =
            new ConcurrentHashMap<>();

    /*
     * Maximum of 10 installation
     * registrations per IP per hour.
     */
    public void checkRegistration(
        String clientIp
    ) {
        check(
            "register:" + clientIp,
            10,
            Duration.ofHours(1)
        );
    }

    /*
     * Maximum of 20 follow/unfollow
     * operations per user every 10 minutes.
     */
    public void checkTeamMutation(
        Long userId
    ) {
        check(
            "team:" + userId,
            20,
            Duration.ofMinutes(10)
        );
    }

    /*
     * Prevent old rate-limit entries from
     * staying in memory forever.
     */
    @Scheduled(
        fixedDelay = 3600000
    )
    public void cleanup() {

        Instant cutoff =
            Instant.now()
                .minus(
                    Duration.ofHours(24)
                );

        windows
            .entrySet()
            .removeIf(
                entry ->
                    entry
                        .getValue()
                        .lastAccess
                        .isBefore(
                            cutoff
                        )
            );
    }

    private void check(
        String key,
        int limit,
        Duration window
    ) {

        Instant now =
            Instant.now();

        AttemptWindow attemptWindow =
            windows.computeIfAbsent(
                key,
                ignored ->
                    new AttemptWindow()
            );

        synchronized (attemptWindow) {

            Instant cutoff =
                now.minus(
                    window
                );

            while (
                !attemptWindow
                    .attempts
                    .isEmpty()
                &&
                !attemptWindow
                    .attempts
                    .peekFirst()
                    .isAfter(
                        cutoff
                    )
            ) {

                attemptWindow
                    .attempts
                    .removeFirst();
            }

            attemptWindow.lastAccess =
                now;

            if (
                attemptWindow
                    .attempts
                    .size()
                    >= limit
            ) {

                throw new ResponseStatusException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    "Too many requests. Please try again later."
                );
            }

            attemptWindow
                .attempts
                .addLast(
                    now
                );
        }
    }
}