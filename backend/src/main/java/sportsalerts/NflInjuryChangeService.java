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

    /*
     * Clear snapshots belonging to an older
     * NFL week.
     *
     * The scheduler calls this BEFORE trying
     * to fetch the new week's injury report.
     *
     * That way, if Week 3 has no report yet,
     * Week 2 data does not remain visible as
     * though it were current.
     */
    @Transactional
    public int clearStaleSnapshots(
        String externalProviderTeamId,
        NflWeekInfo week
    ) {
        return clearStaleSnapshotsInternal(
            externalProviderTeamId,
            week
        );
    }

    @Transactional
    public List<RosterEvent> processTeamInjuries(
        String externalProviderTeamId,
        NflWeekInfo week,
        List<NflInjuryEvent> injuries
    ) {
        List<RosterEvent> newEvents =
            new ArrayList<>();

        /*
         * Keep this here as a second layer of
         * protection in case this method is ever
         * called somewhere besides the scheduler.
         */
        clearStaleSnapshotsInternal(
            externalProviderTeamId,
            week
        );

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
             * First observation for this player
             * during this NFL week.
             *
             * Save as baseline without generating
             * an alert.
             */
            if (existingSnapshot.isEmpty()) {

                NflInjurySnapshot snapshot =
                    new NflInjurySnapshot(
                        externalProviderTeamId,
                        injury.teamName(),
                        injury.playerProviderId(),
                        injury.playerName(),
                        week.seasonYear(),
                        week.seasonType(),
                        week.week(),
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
             * Injury state is unchanged.
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

    private int clearStaleSnapshotsInternal(
        String externalProviderTeamId,
        NflWeekInfo week
    ) {
        List<NflInjurySnapshot>
            existingSnapshots =
                snapshotRepository
                    .findByExternalProviderTeamId(
                        externalProviderTeamId
                    );

        int removed = 0;

        for (
            NflInjurySnapshot snapshot :
            existingSnapshots
        ) {
            boolean sameWeek =
                snapshot.getSeasonYear()
                    != null
                &&
                snapshot.getSeasonYear()
                    == week.seasonYear()
                &&
                week.seasonType().equals(
                    snapshot.getSeasonType()
                )
                &&
                snapshot.getWeekNumber()
                    != null
                &&
                snapshot.getWeekNumber()
                    == week.week();

            if (!sameWeek) {
                snapshotRepository.delete(
                    snapshot
                );

                removed++;
            }
        }

        return removed;
    }

    private String createStateHash(
        NflInjuryEvent injury
    ) {
        /*
         * statusDate is intentionally excluded.
         *
         * A provider date/timestamp changing by
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