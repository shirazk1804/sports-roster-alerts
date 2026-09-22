package sportsalerts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Service;

@Service
public class AppUserService {

    public record RegistrationResult(
        AppUser user,
        String authToken
    ) {
    }

    private static final SecureRandom
        secureRandom =
            new SecureRandom();

    private final AppUserRepository
        appUserRepository;

    public AppUserService(
        AppUserRepository appUserRepository
    ) {
        this.appUserRepository =
            appUserRepository;
    }

    public AppUser getOrCreateUser(
        String installationId
    ) {
        return appUserRepository
            .findByInstallationId(
                installationId
            )
            .orElseGet(
                () ->
                    appUserRepository.save(
                        new AppUser(
                            installationId
                        )
                    )
            );
    }

    public RegistrationResult
        registerInstallation(
            String installationId
        ) {

        AppUser user =
            getOrCreateUser(
                installationId
            );

        /*
         * Existing users created before authentication
         * was added will not have a token hash yet.
         *
         * Issue their first token exactly once.
         */
        if (
            user.getAuthTokenHash() == null ||
            user.getAuthTokenHash().isBlank()
        ) {
            String authToken =
                generateAuthToken();

            String authTokenHash =
                hashAuthToken(
                    authToken
                );

            user.setAuthTokenHash(
                authTokenHash
            );

            appUserRepository.save(
                user
            );

            return new RegistrationResult(
                user,
                authToken
            );
        }

        /*
         * Never regenerate or expose an existing
         * user's token simply because somebody
         * knows the installation ID.
         */
        return new RegistrationResult(
            user,
            null
        );
    }

    public Optional<AppUser>
        authenticateToken(
            String authToken
        ) {

        if (
            authToken == null ||
            authToken.isBlank()
        ) {
            return Optional.empty();
        }

        String authTokenHash =
            hashAuthToken(
                authToken
            );

        return appUserRepository
            .findByAuthTokenHash(
                authTokenHash
            );
    }

    public String extractBearerToken(
        String authorizationHeader
    ) {
        if (
            authorizationHeader == null ||
            !authorizationHeader.startsWith(
                "Bearer "
            )
        ) {
            return null;
        }

        String token =
            authorizationHeader
                .substring(7)
                .trim();

        if (token.isBlank()) {
            return null;
        }

        return token;
    }

    private String generateAuthToken() {

        byte[] randomBytes =
            new byte[32];

        secureRandom.nextBytes(
            randomBytes
        );

        return Base64
            .getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                randomBytes
            );
    }

    private String hashAuthToken(
        String authToken
    ) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            byte[] hash =
                digest.digest(
                    authToken.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return java.util.HexFormat
                .of()
                .formatHex(
                    hash
                );

        } catch (Exception exception) {
            throw new RuntimeException(
                "Could not hash authentication token",
                exception
            );
        }
    }
}