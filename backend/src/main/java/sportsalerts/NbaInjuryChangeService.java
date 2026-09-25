package sportsalerts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NbaInjuryChangeService {

    private final NbaInjurySnapshotRepository
            snapshotRepository;

    private final RosterEventRepository
            rosterEventRepository;

    public NbaInjuryChangeService(
            NbaInjurySnapshotRepository snapshotRepository,
            RosterEventRepository rosterEventRepository) {

        this.snapshotRepository =
                snapshotRepository;

        this.rosterEventRepository =
                rosterEventRepository;
    }

    @Transactional
    public List<RosterEvent> processTeamInjuries(
            String externalProviderTeamId,
            List<NbaInjuryEvent> injuries) {

        List<RosterEvent> newEvents =
                new ArrayList<>();

        Set<String> currentInjuryKeys =
                new HashSet<>();

        for (NbaInjuryEvent injury : injuries) {

            String injuryKey =
                    buildInjuryKey(
                            injury.playerProviderId(),
                            injury.injuryProviderId());

            currentInjuryKeys.add(
                    injuryKey);

            String stateHash =
                    createStateHash(
                            injury);

            Optional<NbaInjurySnapshot>
                    existingSnapshot =
                    snapshotRepository
                            .findByExternalProviderTeamIdAndPlayerProviderIdAndInjuryProviderId(
                                    externalProviderTeamId,
                                    injury.playerProviderId(),
                                    injury.injuryProviderId());

            /*
             * First observation becomes a silent
             * baseline, just like NFL.
             */
            if (existingSnapshot.isEmpty()) {

                NbaInjurySnapshot snapshot =
                        new NbaInjurySnapshot(
                                externalProviderTeamId,
                                injury.teamName(),
                                injury.playerProviderId(),
                                injury.playerName(),
                                injury.position(),
                                injury.injuryProviderId(),
                                injury.injury(),
                                injury.status(),
                                injury.comment(),
                                injury.startDate(),
                                injury.updateDate(),
                                stateHash);

                snapshotRepository.save(
                        snapshot);

                continue;
            }

            NbaInjurySnapshot snapshot =
                    existingSnapshot.get();

            if (stateHash.equals(
                    snapshot.getStateHash())) {

                continue;
            }

            String description =
                    buildChangeDescription(
                            snapshot,
                            injury);

            String dedupeKey =
                    createEventDedupeKey(
                            externalProviderTeamId,
                            injury.playerProviderId(),
                            injury.injuryProviderId(),
                            stateHash);

            boolean alreadyExists =
                    rosterEventRepository
                            .existsByDedupeKey(
                                    dedupeKey);

            snapshot.update(
                    injury.teamName(),
                    injury.playerName(),
                    injury.position(),
                    injury.injury(),
                    injury.status(),
                    injury.comment(),
                    injury.startDate(),
                    injury.updateDate(),
                    stateHash);

            snapshotRepository.save(
                    snapshot);

            if (alreadyExists) {
                continue;
            }

            LocalDate eventDate =
                    parseProviderDate(
                            injury.updateDate());

            String sourceProviderEventId =
                    "nba-injury-"
                            + stateHash;

            RosterEvent rosterEvent =
                    new RosterEvent(
                            "NBA",
                            externalProviderTeamId,
                            injury.teamName(),
                            sourceProviderEventId,
                            injury.playerProviderId(),
                            injury.playerName(),
                            "INJURY_STATUS_CHANGE",
                            eventDate,
                            description,
                            dedupeKey);

            RosterEvent saved =
                    rosterEventRepository.save(
                            rosterEvent);

            newEvents.add(
                    saved);
        }

        /*
         * The NBA endpoint contains active
         * injuries. Remove snapshots that are
         * no longer present so old injuries
         * don't stay visible forever.
         */
        List<NbaInjurySnapshot> existingSnapshots =
                snapshotRepository
                        .findByExternalProviderTeamId(
                                externalProviderTeamId);

        for (
                NbaInjurySnapshot snapshot :
                existingSnapshots) {

            String snapshotKey =
                    buildInjuryKey(
                            snapshot.getPlayerProviderId(),
                            snapshot.getInjuryProviderId());

            if (!currentInjuryKeys.contains(
                    snapshotKey)) {

                snapshotRepository.delete(
                        snapshot);
            }
        }

        return newEvents;
    }

    private String createStateHash(
            NbaInjuryEvent injury) {

        /*
         * updateDate is intentionally excluded.
         * A timestamp changing alone should not
         * generate a notification.
         */
        String rawState =
                normalize(
                        injury.injury())
                        + "|"
                        + normalize(
                                injury.status())
                        + "|"
                        + normalize(
                                injury.comment())
                        + "|"
                        + normalize(
                                injury.startDate());

        return sha256(
                rawState);
    }

    private String createEventDedupeKey(
            String externalProviderTeamId,
            String playerProviderId,
            String injuryProviderId,
            String stateHash) {

        String rawKey =
                "NBA"
                        + "|"
                        + externalProviderTeamId
                        + "|"
                        + playerProviderId
                        + "|"
                        + injuryProviderId
                        + "|INJURY_STATUS_CHANGE|"
                        + stateHash;

        return sha256(
                rawKey);
    }

    private String buildChangeDescription(
            NbaInjurySnapshot previous,
            NbaInjuryEvent current) {

        List<String> changes =
                new ArrayList<>();

        addChange(
                changes,
                "Injury",
                previous.getInjury(),
                current.injury());

        addChange(
                changes,
                "Status",
                previous.getStatus(),
                current.status());

        addChange(
                changes,
                "Details",
                previous.getComment(),
                current.comment());

        if (changes.isEmpty()) {

            return current.playerName()
                    + " injury status updated.";
        }

        return current.playerName()
                + ": "
                + String.join(
                        "; ",
                        changes);
    }

    private void addChange(
            List<String> changes,
            String label,
            String oldValue,
            String newValue) {

        String oldNormalized =
                normalize(
                        oldValue);

        String newNormalized =
                normalize(
                        newValue);

        if (oldNormalized.equals(
                newNormalized)) {

            return;
        }

        String oldDisplay =
                oldNormalized.isBlank()
                        ? "None"
                        : oldValue;

        String newDisplay =
                newNormalized.isBlank()
                        ? "None"
                        : newValue;

        changes.add(
                label
                        + ": "
                        + oldDisplay
                        + " → "
                        + newDisplay);
    }

    private String buildInjuryKey(
            String playerProviderId,
            String injuryProviderId) {

        return playerProviderId
                + "|"
                + injuryProviderId;
    }

    private LocalDate parseProviderDate(
            String value) {

        if (value != null &&
                value.length() >= 10) {

            try {

                return LocalDate.parse(
                        value.substring(
                                0,
                                10));

            } catch (Exception ignored) {
            }
        }

        return LocalDate.now(
                ZoneOffset.UTC);
    }

    private String normalize(
            String value) {

        if (value == null) {
            return "";
        }

        return value
                .trim()
                .toLowerCase();
    }

    private String sha256(
            String value) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256");

            byte[] hash =
                    digest.digest(
                            value.getBytes(
                                    StandardCharsets.UTF_8));

            return HexFormat
                    .of()
                    .formatHex(
                            hash);

        } catch (Exception exception) {

            throw new RuntimeException(
                    "Could not create NBA injury hash",
                    exception);
        }
    }
}