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
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/push-tokens")
public class PushTokenController {

    private final PushTokenRepository pushTokenRepository;

    private final AppUserService appUserService;

    public PushTokenController(
            PushTokenRepository pushTokenRepository,
            AppUserService appUserService) {
        this.pushTokenRepository = pushTokenRepository;

        this.appUserService = appUserService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registerPushToken(
            @RequestHeader(value = "Authorization", required = false) String authorizationHeader,
            @RequestBody Map<String, String> body) {
        AppUser user = appUserService
                .requireAuthenticatedUser(
                        authorizationHeader);

        String token = body.get("token");

        String platform = body.get("platform");

        if (token == null ||
                token.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Push token is required");
        }

        token = token.trim();

        /*
         * Reject obviously invalid or abusive
         * token values before storing them.
         */
        if (token.length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid push token");
        }

        String normalizedPlatform = platform == null
                ? "ios"
                : platform
                        .trim()
                        .toLowerCase();

        if (!normalizedPlatform.equals("ios") &&
                !normalizedPlatform.equals("android")) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid platform");
        }

        PushToken pushToken = pushTokenRepository
                .findByExpoPushToken(
                        token)
                .orElse(null);

        /*
         * If this push token already belongs to a
         * different user, do not silently transfer
         * ownership.
         */
        if (pushToken != null &&
                pushToken.getAppUser() != null &&
                !pushToken
                        .getAppUser()
                        .getId()
                        .equals(
                                user.getId())) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Push token is already registered");
        }

        /*
         * New token for this user.
         */
        if (pushToken == null) {
            pushToken = new PushToken();
        }

        pushToken.setExpoPushToken(
                token);

        pushToken.setPlatform(
                normalizedPlatform);

        pushToken.setAppUser(
                user);

        pushToken.setUpdatedAt(
                LocalDateTime.now());

        pushTokenRepository.save(
                pushToken);
    }
}