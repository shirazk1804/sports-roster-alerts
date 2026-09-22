package sportsalerts;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    private final AppUserService appUserService;

    public AppUserController(
        AppUserService appUserService
    ) {
        this.appUserService = appUserService;
    }

    @PostMapping("/register")
    public AppUser registerUser(
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

        return appUserService.getOrCreateUser(
            installationId
        );
    }
}