package sportsalerts;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NflInjuryChangeService {

    private final NflInjurySnapshotRepository
        snapshotRepository;

    private final RosterEventRepository
        rosterEventRepository;

    public NflInjuryChangeService(
        NflInjurySnapshotRepository snapshotRepository,
        RosterEventRepository rosterEventRepository
    ) {
        this.snapshotRepository =
            snapshotRepository;

        this.rosterEventRepository =
            rosterEventRepository;
    }

    @Transactional
    public List<RosterEvent> processTeamInjuries(
        String externalProviderTeamId,
        List<NflInjuryEvent> injuries
    ) {
        List<RosterEvent> newEvents =
            new ArrayList<>();

        for (
            NflInjuryEvent injury :
            injuries
        ) {
            String stateHash =
                createStateHash(
                    injury
                );

            Optional<NflInjurySnapshot>
                existingSnapshot =
                    snapshotRepository
                        .findByExternalProviderTeamIdAndPlayerProviderId(
                            externalProviderTeamId,
                            injury.playerProviderId()
                        );

            /*
             * First time we've ever seen this
             * player's injury state.
             *
             * Save it as the baseline, but do
             * NOT create an alert.
             */
            if (existingSnapshot.isEmpty()) {

                NflInjurySnapshot snapshot =
                    new NflInjurySnapshot(
                        externalProviderTeamId,
                        injury.teamName(),
                        injury.playerProviderId(),
                        injury.playerName(),
                        injury.injury(),
                        injury.secondaryInjury(),
                        injury.gameStatus(),
                        injury.practiceStatus(),
                        injury.statusDate(),
                        injury.estimatedReturnDate(),
                        stateHash
                    );

                snapshotRepository.save(
                    snapshot
                );

                continue;
            }

            NflInjurySnapshot snapshot =
                existingSnapshot.get();

            /*
             * Nothing meaningful changed.
             */
            if (
                stateHash.equals(
                    snapshot.getStateHash()
                )
            ) {
                continue;
            }

            String description =
                buildChangeDescription(
                    snapshot,
                    injury
                );

            String dedupeKey =
                createEventDedupeKey(
                    externalProviderTeamId,
                    injury.playerProviderId(),
                    stateHash
                );

            boolean alreadyExists =
                rosterEventRepository
                    .existsByDedupeKey(
                        dedupeKey
                    );

            /*
             * Update the snapshot regardless.
             *
             * This ensures the stored snapshot
             * always represents the latest state.
             */
            snapshot.update(
                injury.teamName(),
                injury.playerName(),
                injury.injury(),
                injury.secondaryInjury(),
                injury.gameStatus(),
                injury.practiceStatus(),
                injury.statusDate(),
                injury.estimatedReturnDate(),
                stateHash
            );

            snapshotRepository.save(
                snapshot
            );

            if (alreadyExists) {
                continue;
            }

            LocalDate eventDate =
                parseStatusDate(
                    injury.statusDate()
                );

            /*
             * The injury feed does not give us a
             * transaction GUID like the transaction
             * feed does.
             *
             * Use the state hash as a stable
             * provider-side event identifier.
             */
            String sourceProviderEventId =
                "injury-"
                    + stateHash;

            RosterEvent rosterEvent =
                new RosterEvent(
                    "NFL",
                    externalProviderTeamId,
                    injury.teamName(),
                    sourceProviderEventId,
                    injury.playerProviderId(),
                    injury.playerName(),
                    "INJURY_STATUS_CHANGE",
                    eventDate,
                    description,
                    dedupeKey
                );

            RosterEvent saved =
                rosterEventRepository.save(
                    rosterEvent
                );

            newEvents.add(
                saved
            );
        }

        return newEvents;
    }

    private String createStateHash(
        NflInjuryEvent injury
    ) {
        /*
         * statusDate is intentionally NOT included.
         *
         * A provider timestamp/date changing by
         * itself should not create an alert.
         */
        String rawState =
            normalize(
                injury.injury()
            )
                + "|"
                + normalize(
                    injury.secondaryInjury()
                )
                + "|"
                + normalize(
                    injury.gameStatus()
                )
                + "|"
                + normalize(
                    injury.practiceStatus()
                )
                + "|"
                + normalize(
                    injury.estimatedReturnDate()
                );

        return sha256(
            rawState
        );
    }

    private String createEventDedupeKey(
        String externalProviderTeamId,
        String playerProviderId,
        String stateHash
    ) {
        String rawKey =
            "NFL"
                + "|"
                + externalProviderTeamId
                + "|"
                + playerProviderId
                + "|INJURY_STATUS_CHANGE|"
                + stateHash;

        return sha256(
            rawKey
        );
    }

    private String buildChangeDescription(
        NflInjurySnapshot previous,
        NflInjuryEvent current
    ) {
        List<String> changes =
            new ArrayList<>();

        addChange(
            changes,
            "Injury",
            previous.getInjury(),
            current.injury()
        );

        addChange(
            changes,
            "Secondary injury",
            previous.getSecondaryInjury(),
            current.secondaryInjury()
        );

        addChange(
            changes,
            "Game status",
            previous.getGameStatus(),
            current.gameStatus()
        );

        addChange(
            changes,
            "Practice",
            previous.getPracticeStatus(),
            current.practiceStatus()
        );

        addChange(
            changes,
            "Estimated return",
            previous.getEstimatedReturnDate(),
            current.estimatedReturnDate()
        );

        if (changes.isEmpty()) {
            return current.playerName()
                + " injury status updated.";
        }

        return current.playerName()
            + ": "
            + String.join(
                "; ",
                changes
            );
    }

    private void addChange(
        List<String> changes,
        String label,
        String oldValue,
        String newValue
    ) {
        String oldNormalized =
            normalize(
                oldValue
            );

        String newNormalized =
            normalize(
                newValue
            );

        if (
            oldNormalized.equals(
                newNormalized
            )
        ) {
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
                + newDisplay
        );
    }

    private LocalDate parseStatusDate(
        String value
    ) {
        if (
            value != null &&
            value.length() >= 10
        ) {
            try {
                return LocalDate.parse(
                    value.substring(
                        0,
                        10
                    )
                );
            } catch (Exception ignored) {
            }
        }

        return LocalDate.now(
            ZoneOffset.UTC
        );
    }

    private String normalize(
        String value
    ) {
        if (value == null) {
            return "";
        }

        return value
            .trim()
            .toLowerCase();
    }

    private String sha256(
        String value
    ) {
        try {
            MessageDigest digest =
                MessageDigest.getInstance(
                    "SHA-256"
                );

            byte[] hash =
                digest.digest(
                    value.getBytes(
                        StandardCharsets.UTF_8
                    )
                );

            return HexFormat
                .of()
                .formatHex(
                    hash
                );

        } catch (Exception exception) {

            throw new RuntimeException(
                "Could not create NFL injury hash",
                exception
            );
        }
    }
}