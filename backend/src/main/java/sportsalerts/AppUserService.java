package sportsalerts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AppUserService {

    public record RegistrationResult(
            AppUser user,
            String authToken) {
    }

    private static final SecureRandom secureRandom = new SecureRandom();

    private static final long AUTH_TOKEN_LIFETIME_DAYS = 90;

    private final AppUserRepository appUserRepository;

    public AppUserService(
            AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    public AppUser getOrCreateUser(
            String installationId) {
        return appUserRepository
                .findByInstallationId(
                        installationId)
                .orElseGet(
                        () -> appUserRepository.save(
                                new AppUser(
                                        installationId)));
    }

    public RegistrationResult registerInstallation(
            String installationId) {

        AppUser user = getOrCreateUser(
                installationId);

        LocalDateTime now = LocalDateTime.now();

        /*
         * Existing users created before token
         * expiration was added will have a null
         * expiration date.
         *
         * Keep their existing token valid and
         * give it a 90-day expiration instead of
         * unexpectedly logging them out.
         */
        if (user.getAuthTokenHash() != null &&
                !user.getAuthTokenHash().isBlank() &&
                user.getAuthTokenExpiresAt() == null) {

            user.setAuthTokenExpiresAt(
                    now.plusDays(
                            AUTH_TOKEN_LIFETIME_DAYS));

            appUserRepository.save(
                    user);

            return new RegistrationResult(
                    user,
                    null);
        }

        boolean needsNewToken = user.getAuthTokenHash() == null ||
                user.getAuthTokenHash().isBlank() ||
                user.getAuthTokenExpiresAt() == null ||
                !user
                        .getAuthTokenExpiresAt()
                        .isAfter(
                                now);

        /*
         * New installation or expired token:
         * generate a completely new token.
         */
        if (needsNewToken) {

            String authToken = generateAuthToken();

            String authTokenHash = hashAuthToken(
                    authToken);

            user.setAuthTokenHash(
                    authTokenHash);

            user.setAuthTokenExpiresAt(
                    now.plusDays(
                            AUTH_TOKEN_LIFETIME_DAYS));

            appUserRepository.save(
                    user);

            return new RegistrationResult(
                    user,
                    authToken);
        }

        /*
         * Existing token is still valid.
         */
        return new RegistrationResult(
                user,
                null);
    }

    public Optional<AppUser> authenticateToken(
            String authToken) {

        if (authToken == null ||
                authToken.isBlank()) {
            return Optional.empty();
        }

        String authTokenHash = hashAuthToken(
                authToken);

        Optional<AppUser> userOptional = appUserRepository
                .findByAuthTokenHash(
                        authTokenHash);

        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        AppUser user = userOptional.get();

        LocalDateTime expiresAt = user.getAuthTokenExpiresAt();

        /*
         * Legacy token:
         *
         * Give it an expiration instead of
         * immediately rejecting an existing
         * installation after this upgrade.
         */
        if (expiresAt == null) {

            user.setAuthTokenExpiresAt(
                    LocalDateTime.now()
                            .plusDays(
                                    AUTH_TOKEN_LIFETIME_DAYS));

            appUserRepository.save(
                    user);

            return Optional.of(
                    user);
        }

        /*
         * Expired tokens cannot authenticate.
         */
        if (!expiresAt.isAfter(
                LocalDateTime.now())) {

            return Optional.empty();
        }

        return Optional.of(
                user);
    }

    public AppUser requireAuthenticatedUser(
            String authorizationHeader) {
        String authToken = extractBearerToken(
                authorizationHeader);

        if (authToken == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Authentication required");
        }

        return authenticateToken(
                authToken).orElseThrow(
                        () -> new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "Invalid authentication token"));
    }

    public String extractBearerToken(
            String authorizationHeader) {
        if (authorizationHeader == null ||
                !authorizationHeader.startsWith(
                        "Bearer ")) {
            return null;
        }

        String token = authorizationHeader
                .substring(7)
                .trim();

        if (token.isBlank()) {
            return null;
        }

        return token;
    }

    private String generateAuthToken() {

        byte[] randomBytes = new byte[32];

        secureRandom.nextBytes(
                randomBytes);

        return Base64
                .getUrlEncoder()
                .withoutPadding()
                .encodeToString(
                        randomBytes);
    }

    private String hashAuthToken(
            String authToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256");

            byte[] hash = digest.digest(
                    authToken.getBytes(
                            StandardCharsets.UTF_8));

            return java.util.HexFormat
                    .of()
                    .formatHex(
                            hash);

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Could not hash authentication token",
                    exception);
        }
    }
}