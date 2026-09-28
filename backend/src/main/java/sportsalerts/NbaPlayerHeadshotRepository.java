package sportsalerts;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NbaPlayerHeadshotRepository
        extends JpaRepository<NbaPlayerHeadshot, Long> {

    Optional<NbaPlayerHeadshot>
            findByNbaPlayerIdAndTeamName(
                    String nbaPlayerId,
                    String teamName);
}