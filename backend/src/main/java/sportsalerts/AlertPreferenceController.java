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

    public AlertPreferenceController(
        AlertPreferenceRepository alertPreferenceRepository,
        FollowedTeamRepository followedTeamRepository
    ) {
        this.alertPreferenceRepository =
            alertPreferenceRepository;

        this.followedTeamRepository =
            followedTeamRepository;
    }

    @GetMapping
    public Map<String, Boolean> getPreferences(
        @PathVariable Long teamId
    ) {
        if (!followedTeamRepository.existsById(teamId)) {
            throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Followed team not found"
            );
        }

        Map<String, Boolean> preferences =
            new LinkedHashMap<>();

        alertPreferenceRepository
            .findByFollowedTeamId(teamId)
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
        @RequestBody Map<String, Boolean> settings
    ) {
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
            Map.Entry<String, Boolean>
                entry :
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
}