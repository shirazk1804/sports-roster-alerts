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

    @Test
    void firstObservationCreatesBaselineWithoutAlert() {

        NflInjurySnapshotRepository snapshotRepository =
            mock(NflInjurySnapshotRepository.class);

        RosterEventRepository rosterEventRepository =
            mock(RosterEventRepository.class);

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
                List.of(injury)
            );

        assertTrue(
            events.isEmpty()
        );

        verify(
            snapshotRepository
        ).save(
            any(NflInjurySnapshot.class)
        );

        verify(
            rosterEventRepository,
            never()
        ).save(
            any(RosterEvent.class)
        );
    }

    @Test
    void changedStatusCreatesOneAlertAndThenStopsDuplicating() {

        NflInjurySnapshotRepository snapshotRepository =
            mock(NflInjurySnapshotRepository.class);

        RosterEventRepository rosterEventRepository =
            mock(RosterEventRepository.class);

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
        ).thenReturn(false);

        when(
            rosterEventRepository
                .save(
                    any(RosterEvent.class)
                )
        ).thenAnswer(
            invocation ->
                invocation.getArgument(0)
        );

        List<RosterEvent> firstResult =
            service.processTeamInjuries(
                teamId,
                List.of(changedInjury)
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
         * Run the exact same injury state again.
         *
         * The snapshot was updated during the
         * first call, so this must now produce
         * zero additional events.
         */
        List<RosterEvent> secondResult =
            service.processTeamInjuries(
                teamId,
                List.of(changedInjury)
            );

        assertTrue(
            secondResult.isEmpty()
        );

        verify(
            rosterEventRepository
        ).save(
            any(RosterEvent.class)
        );
    }
}