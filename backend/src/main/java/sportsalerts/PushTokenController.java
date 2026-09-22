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

    private final AppUserService
        appUserService;

    public PushTokenController(
        PushTokenRepository pushTokenRepository,
        AppUserService appUserService
    ) {
        this.pushTokenRepository =
            pushTokenRepository;

        this.appUserService =
            appUserService;
    }

    @PostMapping
    public PushToken registerPushToken(
        @RequestBody Map<String, String> body
    ) {
        String token =
            body.get("token");

        String platform =
            body.get("platform");

        String installationId =
            body.get("installationId");

        if (
            token == null ||
            token.isBlank()
        ) {
            throw new IllegalArgumentException(
                "token is required"
            );
        }

        if (
            installationId == null ||
            installationId.isBlank()
        ) {
            throw new IllegalArgumentException(
                "installationId is required"
            );
        }

        AppUser user =
            appUserService.getOrCreateUser(
                installationId
            );

        PushToken pushToken =
            pushTokenRepository
                .findByExpoPushToken(token)
                .orElseGet(PushToken::new);

        pushToken.setExpoPushToken(
            token
        );

        pushToken.setPlatform(
            platform == null
                ? "ios"
                : platform
        );

        pushToken.setAppUser(
            user
        );

        pushToken.setUpdatedAt(
            LocalDateTime.now()
        );

        return pushTokenRepository.save(
            pushToken
        );
    }

    @GetMapping("/count")
    public Map<String, Long> getCount() {
        return Map.of(
            "count",
            pushTokenRepository.count()
        );
    }
}