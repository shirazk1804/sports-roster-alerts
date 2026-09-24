package sportsalerts;

import java.util.function.Supplier;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;

@Component
public class SportradarRequestLimiter {

    /*
     * Trial limit is 1 request per second.
     * Give ourselves a little extra room.
     */
    private static final long MIN_INTERVAL_MS =
        1200;

    private static final int MAX_ATTEMPTS =
        4;

    private long lastRequestStartedAt =
        0L;

    public synchronized <T> T execute(
        Supplier<T> request
    ) {

        int attempt = 0;

        while (true) {

            waitForAvailableSlot();

            /*
             * Record when this request starts,
             * so another Sportradar request
             * cannot begin too soon.
             */
            lastRequestStartedAt =
                System.currentTimeMillis();

            try {
                return request.get();

            } catch (
                HttpClientErrorException
                    .TooManyRequests exception
            ) {

                attempt++;

                if (
                    attempt >=
                    MAX_ATTEMPTS
                ) {
                    throw exception;
                }

                long backoffMs =
                    switch (attempt) {
                        case 1 -> 2000;
                        case 2 -> 4000;
                        default -> 8000;
                    };

                System.err.println(
                    "Sportradar rate limit hit. "
                        + "Retrying in "
                        + backoffMs
                        + " ms."
                );

                sleep(
                    backoffMs
                );
            }
        }
    }

    private void waitForAvailableSlot() {

        long now =
            System.currentTimeMillis();

        long elapsed =
            now -
            lastRequestStartedAt;

        long waitMs =
            MIN_INTERVAL_MS -
            elapsed;

        if (waitMs > 0) {
            sleep(
                waitMs
            );
        }
    }

    private void sleep(
        long milliseconds
    ) {
        try {
            Thread.sleep(
                milliseconds
            );

        } catch (
            InterruptedException exception
        ) {
            Thread.currentThread()
                .interrupt();

            throw new RuntimeException(
                "Sportradar request wait interrupted",
                exception
            );
        }
    }
}