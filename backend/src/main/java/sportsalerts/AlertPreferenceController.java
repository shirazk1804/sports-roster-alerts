package sportsalerts;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping(
    "/api/followed-teams/{teamId}/alert-preferences"
)
public class AlertPreferenceController {

    private final AlertPreferenceRepository
        alertPreferenceRepository;

    private final FollowedTeamRepository
        followedTeamRepository;

    private final AppUserService
        appUserService;

    public AlertPreferenceController(
        AlertPreferenceRepository alertPreferenceRepository,
        FollowedTeamRepository followedTeamRepository,
        AppUserService appUserService
    ) {
        this.alertPreferenceRepository =
            alertPreferenceRepository;

        this.followedTeamRepository =
            followedTeamRepository;

        this.appUserService =
            appUserService;
    }

    @GetMapping
    public Map<String, Boolean> getPreferences(
        @PathVariable Long teamId,
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        String authorizationHeader
    ) {
        FollowedTeam team =
            getOwnedFollowedTeam(
                teamId,
                authorizationHeader
            );

        Map<String, Boolean> preferences =
            new LinkedHashMap<>();

        alertPreferenceRepository
            .findByFollowedTeamId(
                team.getId()
            )
            .forEach(
                preference ->
                    preferences.put(
                        preference.getAlertKey(),
                        preference.isEnabled()
                    )
            );

        return preferences;
    }

    @PutMapping
    @Transactional
    public Map<String, Boolean> savePreferences(
        @PathVariable Long teamId,
        @RequestHeader(
            value = "Authorization",
            required = false
        )
        String authorizationHeader,
        @RequestBody Map<String, Boolean> settings
    ) {
        FollowedTeam team =
            getOwnedFollowedTeam(
                teamId,
                authorizationHeader
            );

        List<AlertPreference>
            existingPreferences =
                alertPreferenceRepository
                    .findByFollowedTeamId(
                        teamId
                    );

        Map<String, AlertPreference>
            existingByKey =
                existingPreferences
                    .stream()
                    .collect(
                        Collectors.toMap(
                            AlertPreference::getAlertKey,
                            preference ->
                                preference
                        )
                    );

        for (
            Map.Entry<String, Boolean> entry :
            settings.entrySet()
        ) {
            String alertKey =
                entry.getKey();

            Boolean enabled =
                entry.getValue();

            AlertPreference preference =
                existingByKey.get(
                    alertKey
                );

            if (preference == null) {
                preference =
                    new AlertPreference(
                        team,
                        alertKey,
                        enabled
                    );
            } else {
                preference.setEnabled(
                    enabled
                );
            }

            alertPreferenceRepository.save(
                preference
            );
        }

        return settings;
    }

    private FollowedTeam getOwnedFollowedTeam(
        Long teamId,
        String authorizationHeader
    ) {
        AppUser user =
            appUserService
                .requireAuthenticatedUser(
                    authorizationHeader
                );

        FollowedTeam team =
            followedTeamRepository
                .findById(teamId)
                .orElseThrow(
                    () ->
                        new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Followed team not found"
                        )
                );

        if (
            team.getAppUser() == null ||
            !team
                .getAppUser()
                .getId()
                .equals(user.getId())
        ) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Followed team not found"
            );
        }

        return team;
    }
}