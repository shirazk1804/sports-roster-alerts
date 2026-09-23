package sportsalerts;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NflInjurySnapshotRepository
    extends JpaRepository<NflInjurySnapshot, Long> {

    Optional<NflInjurySnapshot>
        findByExternalProviderTeamIdAndPlayerProviderId(
            String externalProviderTeamId,
            String playerProviderId
        );

    List<NflInjurySnapshot>
        findByExternalProviderTeamId(
            String externalProviderTeamId
        );

    List<NflInjurySnapshot>
        findByExternalProviderTeamIdOrderByPlayerNameAsc(
            String externalProviderTeamId
        );
}