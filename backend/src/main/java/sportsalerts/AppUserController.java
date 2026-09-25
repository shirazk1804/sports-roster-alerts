package sportsalerts;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/users")
public class AppUserController {

    public record RegistrationResponse(
            Long id,
            String installationId,
            LocalDateTime createdAt,
            String authToken) {
    }

    private final AppUserService appUserService;

    private final ApiRateLimitService apiRateLimitService;

    public AppUserController(
            AppUserService appUserService,
            ApiRateLimitService apiRateLimitService) {

        this.appUserService = appUserService;

        this.apiRateLimitService = apiRateLimitService;
    }

    @PostMapping("/register")
    public RegistrationResponse registerUser(
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        apiRateLimitService
                .checkRegistration(
                        getClientIp(
                                request));

        String installationId = body == null
                ? null
                : body.get(
                        "installationId");

        if (installationId == null ||
                installationId.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "installationId is required");
        }

        installationId = installationId.trim();

        /*
         * The database column allows a maximum
         * of 100 characters.
         */
        if (installationId.length() < 10 ||
                installationId.length() > 100) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid installationId");
        }

        /*
         * Our app generates installation IDs
         * using letters, numbers and hyphens.
         *
         * Reject unusual input before it reaches
         * the database.
         */
        if (!installationId.matches(
                "[A-Za-z0-9-]+")) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid installationId");
        }

        AppUserService.RegistrationResult result = appUserService.registerInstallation(
                installationId);

        AppUser user = result.user();

        return new RegistrationResponse(
                user.getId(),
                user.getInstallationId(),
                user.getCreatedAt(),
                result.authToken());
    }

    private String getClientIp(
            HttpServletRequest request) {

        String realIp = request.getHeader(
                "X-Real-IP");

        if (realIp != null &&
                !realIp.isBlank()) {

            return realIp.trim();
        }

        return request.getRemoteAddr();
    }
}