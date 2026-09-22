package sportsalerts;

import org.springframework.stereotype.Service;

@Service
public class AppUserService {

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
}