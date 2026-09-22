package sportsalerts;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/push-tokens")
public class PushTokenController {

    private final PushTokenRepository
        pushTokenRepository;

    public PushTokenController(
        PushTokenRepository pushTokenRepository
    ) {
        this.pushTokenRepository =
            pushTokenRepository;
    }

    @PostMapping
    public Map<String, Object>
        registerPushToken(
            @RequestBody
            Map<String, String> request
        ) {

        String token =
            request.get("token");

        String platform =
            request.getOrDefault(
                "platform",
                "unknown"
            );

        if (
            token == null ||
            token.isBlank()
        ) {
            throw new IllegalArgumentException(
                "Push token is required"
            );
        }

        PushToken pushToken =
            pushTokenRepository
                .findByExpoPushToken(token)
                .orElse(
                    new PushToken(
                        token,
                        platform
                    )
                );

        pushToken.setPlatform(
            platform
        );

        pushToken.setUpdatedAt(
            LocalDateTime.now()
        );

        pushTokenRepository.save(
            pushToken
        );

        return Map.of(
            "saved",
            true
        );
    }

    @GetMapping("/count")
    public Map<String, Long>
        getTokenCount() {

        return Map.of(
            "count",
            pushTokenRepository.count()
        );
    }
}