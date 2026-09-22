package sportsalerts;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    public record RegistrationResponse(
        Long id,
        String installationId,
        LocalDateTime createdAt,
        String authToken
    ) {
    }

    private final AppUserService
        appUserService;

    public AppUserController(
        AppUserService appUserService
    ) {
        this.appUserService =
            appUserService;
    }

    @PostMapping("/register")
    public RegistrationResponse registerUser(
        @RequestBody Map<String, String> body
    ) {
        String installationId =
            body.get("installationId");

        if (
            installationId == null ||
            installationId.isBlank()
        ) {
            throw new IllegalArgumentException(
                "installationId is required"
            );
        }

        AppUserService.RegistrationResult result =
            appUserService.registerInstallation(
                installationId
            );

        AppUser user =
            result.user();

        return new RegistrationResponse(
            user.getId(),
            user.getInstallationId(),
            user.getCreatedAt(),
            result.authToken()
        );
    }
}