package sportsalerts;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerPushToken(
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        String authorizationHeader,
        @RequestBody Map<String, String> body
    ) {
        AppUser user =
            appUserService
                .requireAuthenticatedUser(
                    authorizationHeader
                );

        String token =
            body.get("token");

        String platform =
            body.get("platform");

        if (
            token == null ||
            token.isBlank()
        ) {
            throw new IllegalArgumentException(
                "token is required"
            );
        }

        PushToken pushToken =
            pushTokenRepository
                .findByExpoPushToken(token)
                .orElseGet(
                    PushToken::new
                );

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

        pushTokenRepository.save(
            pushToken
        );
    }
}