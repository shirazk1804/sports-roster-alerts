package sportsalerts;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

public class NflInjuryChangeServiceTest {

    private final NflWeekInfo week =
        new NflWeekInfo(
            2026,
            "REG",
            3
        );

    @Test
    void firstObservationCreatesBaselineWithoutAlert() {

        NflInjurySnapshotRepository snapshotRepository =
            mock(
                NflInjurySnapshotRepository.class
            );

        RosterEventRepository rosterEventRepository =
            mock(
                RosterEventRepository.class
            );

        NflInjuryChangeService service =
            new NflInjuryChangeService(
                snapshotRepository,
                rosterEventRepository
            );

        String teamId =
            "test-team-id";

        NflInjuryEvent injury =
            new NflInjuryEvent(
                teamId,
                "Los Angeles Rams",
                "test-player-id",
                "Test Player",
                "Hamstring",
                "",
                "Questionable",
                "Limited",
                "2026-09-23",
                "",
                "Test Player - Hamstring - Questionable - Limited"
            );

        when(
            snapshotRepository
                .findByExternalProviderTeamId(
                    teamId
                )
        ).thenReturn(
            List.of()
        );

        when(
            snapshotRepository
                .findByExternalProviderTeamIdAndPlayerProviderId(
                    teamId,
                    "test-player-id"
                )
        ).thenReturn(
            Optional.empty()
        );

        List<RosterEvent> events =
            service.processTeamInjuries(
                teamId,
                week,
                List.of(injury)
            );

        assertTrue(
            events.isEmpty()
        );

        verify(
            snapshotRepository
        ).save(
            any(
                NflInjurySnapshot.class
            )
        );

        verify(
            rosterEventRepository,
            never()
        ).save(
            any(
                RosterEvent.class
            )
        );
    }

    @Test
    void changedStatusCreatesOneAlertAndThenStopsDuplicating() {

        NflInjurySnapshotRepository snapshotRepository =
            mock(
                NflInjurySnapshotRepository.class
            );

        RosterEventRepository rosterEventRepository =
            mock(
                RosterEventRepository.class
            );

        NflInjuryChangeService service =
            new NflInjuryChangeService(
                snapshotRepository,
                rosterEventRepository
            );

        String teamId =
            "test-team-id";

        String playerId =
            "test-player-id";

        NflInjurySnapshot existingSnapshot =
            new NflInjurySnapshot(
                teamId,
                "Los Angeles Rams",
                playerId,
                "Test Player",
                2026,
                "REG",
                3,
                "Hamstring",
                "",
                "Questionable",
                "Limited",
                "2026-09-22",
                "",
                "old-state-hash"
            );

        NflInjuryEvent changedInjury =
            new NflInjuryEvent(
                teamId,
                "Los Angeles Rams",
                playerId,
                "Test Player",
                "Hamstring",
                "",
                "Out",
                "Did Not Participate",
                "2026-09-23",
                "",
                "Test Player - Hamstring - Out - Did Not Participate"
            );

        when(
            snapshotRepository
                .findByExternalProviderTeamId(
                    teamId
                )
        ).thenReturn(
            List.of(
                existingSnapshot
            )
        );

        when(
            snapshotRepository
                .findByExternalProviderTeamIdAndPlayerProviderId(
                    teamId,
                    playerId
                )
        ).thenReturn(
            Optional.of(
                existingSnapshot
            )
        );

        when(
            rosterEventRepository
                .existsByDedupeKey(
                    anyString()
                )
        ).thenReturn(
            false
        );

        when(
            rosterEventRepository
                .save(
                    any(
                        RosterEvent.class
                    )
                )
        ).thenAnswer(
            invocation ->
                invocation.getArgument(0)
        );

        List<RosterEvent> firstResult =
            service.processTeamInjuries(
                teamId,
                week,
                List.of(
                    changedInjury
                )
            );

        assertEquals(
            1,
            firstResult.size()
        );

        RosterEvent event =
            firstResult.get(0);

        assertEquals(
            "NFL",
            event.getLeague()
        );

        assertEquals(
            "INJURY_STATUS_CHANGE",
            event.getEventType()
        );

        assertEquals(
            "Test Player",
            event.getPlayerName()
        );

        assertTrue(
            event.getDescription()
                .contains(
                    "Game status: Questionable → Out"
                )
        );

        /*
         * The first call updated the existing
         * snapshot to Out.
         *
         * Running the same state again should
         * therefore create no second event.
         */
        List<RosterEvent> secondResult =
            service.processTeamInjuries(
                teamId,
                week,
                List.of(
                    changedInjury
                )
            );

        assertTrue(
            secondResult.isEmpty()
        );

        verify(
            rosterEventRepository
        ).save(
            any(
                RosterEvent.class
            )
        );
    }

    @Test
    void oldWeekSnapshotIsRemovedAndNewWeekBecomesSilentBaseline() {

        NflInjurySnapshotRepository snapshotRepository =
            mock(
                NflInjurySnapshotRepository.class
            );

        RosterEventRepository rosterEventRepository =
            mock(
                RosterEventRepository.class
            );

        NflInjuryChangeService service =
            new NflInjuryChangeService(
                snapshotRepository,
                rosterEventRepository
            );

        String teamId =
            "test-team-id";

        String playerId =
            "test-player-id";

        NflInjurySnapshot oldWeekSnapshot =
            new NflInjurySnapshot(
                teamId,
                "Los Angeles Rams",
                playerId,
                "Test Player",
                2026,
                "REG",
                2,
                "Hamstring",
                "",
                "Out",
                "Did Not Participate",
                "2026-09-19",
                "",
                "old-week-hash"
            );

        NflInjuryEvent weekThreeInjury =
            new NflInjuryEvent(
                teamId,
                "Los Angeles Rams",
                playerId,
                "Test Player",
                "Hamstring",
                "",
                "Questionable",
                "Limited",
                "2026-09-23",
                "",
                "Test Player - Hamstring - Questionable - Limited"
            );

        when(
            snapshotRepository
                .findByExternalProviderTeamId(
                    teamId
                )
        ).thenReturn(
            List.of(
                oldWeekSnapshot
            )
        );

        /*
         * Once the old Week 2 snapshot is
         * removed, Week 3 should be treated
         * as a brand-new baseline.
         */
        when(
            snapshotRepository
                .findByExternalProviderTeamIdAndPlayerProviderId(
                    teamId,
                    playerId
                )
        ).thenReturn(
            Optional.empty()
        );

        List<RosterEvent> events =
            service.processTeamInjuries(
                teamId,
                week,
                List.of(
                    weekThreeInjury
                )
            );

        verify(
            snapshotRepository
        ).delete(
            oldWeekSnapshot
        );

        verify(
            snapshotRepository
        ).save(
            any(
                NflInjurySnapshot.class
            )
        );

        assertTrue(
            events.isEmpty()
        );

        verify(
            rosterEventRepository,
            never()
        ).save(
            any(
                RosterEvent.class
            )
        );
    }
}