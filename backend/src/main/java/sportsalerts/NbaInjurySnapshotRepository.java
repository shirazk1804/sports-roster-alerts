package sportsalerts;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NbaInjurySnapshotRepository
        extends JpaRepository<
                NbaInjurySnapshot,
                Long> {

    Optional<NbaInjurySnapshot>
            findByExternalProviderTeamIdAndPlayerProviderIdAndInjuryProviderId(
                    String externalProviderTeamId,
                    String playerProviderId,
                    String injuryProviderId);

    List<NbaInjurySnapshot>
            findByExternalProviderTeamId(
                    String externalProviderTeamId);

    List<NbaInjurySnapshot>
            findByExternalProviderTeamIdOrderByPlayerNameAsc(
                    String externalProviderTeamId);
}