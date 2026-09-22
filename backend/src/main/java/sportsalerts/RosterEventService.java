package sportsalerts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RosterEventService {

    private final RosterEventRepository
        rosterEventRepository;

    private final FollowedTeamRepository
        followedTeamRepository;

    private final AlertPreferenceRepository
        alertPreferenceRepository;

    public RosterEventService(
        RosterEventRepository rosterEventRepository,
        FollowedTeamRepository followedTeamRepository,
        AlertPreferenceRepository alertPreferenceRepository
    ) {
        this.rosterEventRepository =
            rosterEventRepository;

        this.followedTeamRepository =
            followedTeamRepository;

        this.alertPreferenceRepository =
            alertPreferenceRepository;
    }

    @Transactional
    public List<RosterEvent> saveMlbEvents(
        Long teamId,
        List<MlbTransactionEvent> events
    ) {
        List<RosterEvent> savedEvents =
            new ArrayList<>();

        for (
            MlbTransactionEvent event :
            events
        ) {
            String dedupeKey =
                createDedupeKey(
                    teamId,
                    event
                );

            boolean alreadyExists =
                rosterEventRepository
                    .existsByDedupeKey(
                        dedupeKey
                    );

            if (alreadyExists) {
                continue;
            }

            RosterEvent rosterEvent =
                new RosterEvent(
                    "MLB",
                    teamId,
                    event.teamName(),
                    event.sourceTransactionId(),
                    event.playerId(),
                    event.playerName(),
                    event.eventType(),
                    LocalDate.parse(
                        event.date()
                    ),
                    event.description(),
                    dedupeKey
                );

            RosterEvent saved =
                rosterEventRepository.save(
                    rosterEvent
                );

            savedEvents.add(
                saved
            );
        }

        return savedEvents;
    }

    public List<RosterEvent>
        getVisibleEventsForUser(
            Long appUserId
        ) {

        List<FollowedTeam> followedTeams =
            followedTeamRepository
                .findByAppUserId(
                    appUserId
                );

        if (followedTeams.isEmpty()) {
            return List.of();
        }

        /*
         * Map each followed MLB team name
         * to this user's FollowedTeam row.
         *
         * Example:
         *
         * "Los Angeles Dodgers"
         *      -> User 1's Dodgers follow
         */
        Map<String, FollowedTeam>
            followedMlbTeamsByName =
                followedTeams
                    .stream()
                    .filter(
                        team ->
                            "MLB".equals(
                                team.getLeague()
                            )
                    )
                    .collect(
                        Collectors.toMap(
                            FollowedTeam::getName,
                            team -> team
                        )
                    );

        if (
            followedMlbTeamsByName
                .isEmpty()
        ) {
            return List.of();
        }

        List<RosterEvent> allEvents =
            rosterEventRepository
                .findAllByOrderByEventDateDescIdDesc();

        List<RosterEvent> visibleEvents =
            new ArrayList<>();

        for (
            RosterEvent event :
            allEvents
        ) {
            if (
                !"MLB".equals(
                    event.getLeague()
                )
            ) {
                continue;
            }

            FollowedTeam followedTeam =
                followedMlbTeamsByName.get(
                    event.getTeamName()
                );

            if (followedTeam == null) {
                continue;
            }

            if (
                shouldNotify(
                    followedTeam,
                    event
                )
            ) {
                visibleEvents.add(
                    event
                );
            }
        }

        return visibleEvents;
    }

    public boolean shouldNotify(
        FollowedTeam followedTeam,
        RosterEvent event
    ) {
        List<AlertPreference> preferences =
            alertPreferenceRepository
                .findByFollowedTeamId(
                    followedTeam.getId()
                );

        if (preferences.isEmpty()) {
            return true;
        }

        Map<String, Boolean>
            preferenceMap =
                preferences
                    .stream()
                    .collect(
                        Collectors.toMap(
                            AlertPreference
                                ::getAlertKey,
                            AlertPreference
                                ::isEnabled
                        )
                    );

        String preferenceKey =
            getPreferenceKey(
                event.getEventType()
            );

        if (preferenceKey == null) {
            return true;
        }

        return preferenceMap.getOrDefault(
            preferenceKey,
            true
        );
    }

    private String getPreferenceKey(
        String eventType
    ) {
        return switch (eventType) {

            case "IL_PLACEMENT" ->
                "IL placements";

            case "IL_ACTIVATION" ->
                "IL activations";

            case "IL_TRANSFER" ->
                "IL placements";

            case "RECALLED" ->
                "Call-ups";

            case "OPTIONED" ->
                "Options to minors";

            case "DESIGNATED_FOR_ASSIGNMENT" ->
                "Designated for assignment";

            case "TRADE" ->
                "Trades";

            case "CONTRACT_SELECTED" ->
                "Call-ups";

            case "OUTRIGHTED" ->
                "Options to minors";

            case "REHAB_ASSIGNMENT" ->
                "Injury status changes";

            case "BEREAVEMENT_PLACEMENT",
                 "BEREAVEMENT_ACTIVATION",
                 "PATERNITY_PLACEMENT",
                 "PATERNITY_ACTIVATION",
                 "RESTRICTED_LIST_PLACEMENT",
                 "RESTRICTED_LIST_ACTIVATION",
                 "SUSPENDED",
                 "SUSPENSION_REINSTATED",
                 "ROSTER_ACTIVATION"
                ->
                "Injury status changes";

            case "RELEASED",
                 "SIGNED",
                 "MINOR_LEAGUE_SIGNING",
                 "WAIVER_CLAIM",
                 "WAIVERS",
                 "RETIRED"
                ->
                "Signings and releases";

            default ->
                null;
        };
    }

    private String createDedupeKey(
        Long teamId,
        MlbTransactionEvent event
    ) {
        String rawKey =
            teamId
                + "|"
                + event.playerId()
                + "|"
                + event.eventType()
                + "|"
                + event.date()
                + "|"
                + event.description();

        try {
            MessageDigest digest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            byte[] hash =
                digest.digest(
                    rawKey.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return HexFormat
                .of()
                .formatHex(hash);

        } catch (Exception exception) {

            throw new RuntimeException(
                "Could not create event dedupe key",
                exception
            );
        }
    }
}