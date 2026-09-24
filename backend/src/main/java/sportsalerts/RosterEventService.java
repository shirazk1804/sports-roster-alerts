package sportsalerts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RosterEventService {

    private final RosterEventRepository rosterEventRepository;

    private final FollowedTeamRepository followedTeamRepository;

    private final AlertPreferenceRepository alertPreferenceRepository;

    public RosterEventService(
            RosterEventRepository rosterEventRepository,
            FollowedTeamRepository followedTeamRepository,
            AlertPreferenceRepository alertPreferenceRepository) {
        this.rosterEventRepository = rosterEventRepository;

        this.followedTeamRepository = followedTeamRepository;

        this.alertPreferenceRepository = alertPreferenceRepository;
    }

    // =========================
    // MLB
    // =========================

    @Transactional
    public List<RosterEvent> saveMlbEvents(
            Long teamId,
            List<MlbTransactionEvent> events) {
        List<RosterEvent> savedEvents = new ArrayList<>();

        for (MlbTransactionEvent event : events) {
            String dedupeKey = createMlbDedupeKey(
                    teamId,
                    event);

            boolean alreadyExists = rosterEventRepository
                    .existsByDedupeKey(
                            dedupeKey);

            if (alreadyExists) {
                continue;
            }

            RosterEvent rosterEvent = new RosterEvent(
                    "MLB",
                    teamId,
                    event.teamName(),
                    event.sourceTransactionId(),
                    event.playerId(),
                    event.playerName(),
                    event.eventType(),
                    LocalDate.parse(
                            event.date()),
                    event.description(),
                    dedupeKey);

            RosterEvent saved = rosterEventRepository.save(
                    rosterEvent);

            savedEvents.add(
                    saved);
        }

        return savedEvents;
    }

    @Transactional
    public RosterEvent saveMlbLineupPostedEvent(
            Long teamId,
            String teamName,
            Long gamePk,
            String opponentName,
            String homeAway,
            LocalDate eventDate) {

        String rawKey = "MLB"
                + "|LINEUP_POSTED"
                + "|"
                + teamId
                + "|"
                + gamePk;

        String dedupeKey = sha256(
                rawKey);

        if (rosterEventRepository
                .existsByDedupeKey(
                        dedupeKey)) {

            return null;
        }

        String matchup = "";

        if (opponentName != null &&
                !opponentName.isBlank()) {

            if ("HOME".equals(
                    homeAway)) {

                matchup = " vs "
                        + opponentName;

            } else if ("AWAY".equals(
                    homeAway)) {

                matchup = " at "
                        + opponentName;

            } else {

                matchup = " against "
                        + opponentName;
            }
        }

        String description = "Today's starting lineup has been posted"
                + matchup
                + ".";

        RosterEvent rosterEvent = new RosterEvent(
                "MLB",
                teamId,
                teamName,
                (Long) null,
                (Long) null,
                "Starting Lineup",
                "STARTING_LINEUP_POSTED",
                eventDate,
                description,
                dedupeKey);

        return rosterEventRepository
                .save(
                        rosterEvent);
    }

    // =========================
    // NFL
    // =========================

    @Transactional
    public List<RosterEvent> saveNflEvents(
            String externalProviderTeamId,
            List<NflTransactionEvent> events) {
        List<RosterEvent> savedEvents = new ArrayList<>();

        for (NflTransactionEvent event : events) {
            String dedupeKey = createNflDedupeKey(
                    externalProviderTeamId,
                    event);

            boolean alreadyExists = rosterEventRepository
                    .existsByDedupeKey(
                            dedupeKey);

            if (alreadyExists) {
                continue;
            }

            LocalDate eventDate = parseProviderDate(
                    event.effectiveDate());

            RosterEvent rosterEvent = new RosterEvent(
                    "NFL",
                    externalProviderTeamId,
                    event.teamName(),
                    event.sourceProviderEventId(),
                    event.playerProviderId(),
                    event.playerName(),
                    event.eventType(),
                    eventDate,
                    event.description(),
                    dedupeKey);

            RosterEvent saved = rosterEventRepository.save(
                    rosterEvent);

            savedEvents.add(
                    saved);
        }

        return savedEvents;
    }

    // =========================
    // USER VISIBLE EVENTS
    // =========================

    public List<RosterEvent> getVisibleEventsForUser(
            Long appUserId) {

        List<FollowedTeam> followedTeams = followedTeamRepository
                .findByAppUserId(
                        appUserId);

        if (followedTeams.isEmpty()) {
            return List.of();
        }

        /*
         * Key format:
         *
         * MLB|Los Angeles Dodgers
         * NFL|Los Angeles Rams
         *
         * This prevents teams in different
         * leagues from ever colliding.
         */
        Map<String, FollowedTeam> followedTeamsByLeagueAndName = followedTeams
                .stream()
                .collect(
                        Collectors.toMap(
                                team -> buildTeamKey(
                                        team.getLeague(),
                                        team.getName()),
                                team -> team));

        List<RosterEvent> allEvents = rosterEventRepository
                .findAllByOrderByEventDateDescIdDesc();

        List<RosterEvent> visibleEvents = new ArrayList<>();

        for (RosterEvent event : allEvents) {
            String eventKey = buildTeamKey(
                    event.getLeague(),
                    event.getTeamName());

            FollowedTeam followedTeam = followedTeamsByLeagueAndName
                    .get(eventKey);

            if (followedTeam == null) {
                continue;
            }

            if (shouldNotify(
                    followedTeam,
                    event)) {
                visibleEvents.add(
                        event);
            }
        }

        return visibleEvents;
    }

    // =========================
    // ALERT PREFERENCES
    // =========================

    public boolean shouldNotify(
            FollowedTeam followedTeam,
            RosterEvent event) {
        List<AlertPreference> preferences = alertPreferenceRepository
                .findByFollowedTeamId(
                        followedTeam.getId());

        if (preferences.isEmpty()) {
            return true;
        }

        Map<String, Boolean> preferenceMap = preferences
                .stream()
                .collect(
                        Collectors.toMap(
                                AlertPreference::getAlertKey,
                                AlertPreference::isEnabled));

        String preferenceKey = getPreferenceKey(
                event.getLeague(),
                event.getEventType());

        /*
         * We intentionally allow unknown event
         * types for now instead of silently
         * suppressing them.
         *
         * Once we inspect the exact NFL preference
         * names in the mobile app, we'll map every
         * NFL event type to its correct switch.
         */
        if (preferenceKey == null) {
            return true;
        }

        return preferenceMap.getOrDefault(
                preferenceKey,
                true);
    }

    private String getPreferenceKey(
            String league,
            String eventType) {

        if ("NFL".equals(league)) {

            return switch (eventType) {

                case "INJURY_STATUS_CHANGE" ->
                    "Injury status changes";

                case "INJURED_RESERVE",
                        "IR_DESIGNATED_RETURN",
                        "PUP_PLACEMENT",
                        "NFI_PLACEMENT" ->
                    "IR / PUP moves";

                case "ACTIVATED" ->
                    "Activations";

                case "SIGNED",
                        "RELEASED",
                        "WAIVED",
                        "RETIRED" ->
                    "Signings and releases";

                case "TRADE" ->
                    "Trades";

                case "PRACTICE_SQUAD" ->
                    "Practice squad moves";

                case "SUSPENDED",
                        "SUSPENSION_REINSTATED" ->
                    "Suspensions";

                default ->
                    null;
            };
        }

        if ("MLB".equals(league)) {

            return switch (eventType) {
                
                case "STARTING_LINEUP_POSTED" ->
                    "Starting lineup posted";

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
                        "ROSTER_ACTIVATION" ->
                    "Injury status changes";

                case "RELEASED",
                        "SIGNED",
                        "MINOR_LEAGUE_SIGNING",
                        "WAIVER_CLAIM",
                        "WAIVERS",
                        "RETIRED" ->
                    "Signings and releases";

                default ->
                    null;
            };
        }

        return null;
    }

    // =========================
    // DEDUPLICATION
    // =========================

    private String createMlbDedupeKey(
            Long teamId,
            MlbTransactionEvent event) {
        String rawKey = teamId
                + "|"
                + event.playerId()
                + "|"
                + event.eventType()
                + "|"
                + event.date()
                + "|"
                + event.description();

        return sha256(
                rawKey);
    }

    private String createNflDedupeKey(
            String externalProviderTeamId,
            NflTransactionEvent event) {
        String rawKey = "NFL"
                + "|"
                + externalProviderTeamId
                + "|"
                + event.sourceProviderEventId()
                + "|"
                + event.playerProviderId()
                + "|"
                + event.eventType()
                + "|"
                + event.effectiveDate()
                + "|"
                + event.description();

        return sha256(
                rawKey);
    }

    private String sha256(
            String rawKey) {
        try {
            MessageDigest digest = MessageDigest.getInstance(
                    "SHA-256");

            byte[] hash = digest.digest(
                    rawKey.getBytes(
                            StandardCharsets.UTF_8));

            return HexFormat
                    .of()
                    .formatHex(
                            hash);

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Could not create event dedupe key",
                    exception);
        }
    }

    // =========================
    // HELPERS
    // =========================

    private LocalDate parseProviderDate(
            String value) {
        if (value == null ||
                value.isBlank()) {
            return LocalDate.now(
                    ZoneOffset.UTC);
        }

        /*
         * Handles either:
         *
         * 2026-09-23
         *
         * or timestamps beginning with:
         *
         * 2026-09-23T...
         */
        if (value.length() >= 10) {
            return LocalDate.parse(
                    value.substring(
                            0,
                            10));
        }

        return LocalDate.now(
                ZoneOffset.UTC);
    }

    private String buildTeamKey(
            String league,
            String teamName) {
        return league
                + "|"
                + teamName;
    }
}