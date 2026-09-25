package sportsalerts;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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

        String installationId = body.get("installationId");

        if (installationId == null ||
                installationId.isBlank()) {
            throw new IllegalArgumentException(
                    "installationId is required");
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