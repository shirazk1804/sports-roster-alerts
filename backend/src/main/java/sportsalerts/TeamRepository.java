package sportsalerts;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TeamRepository
    extends JpaRepository<Team, Long> {

    Optional<Team> findByLeagueAndName(
        String league,
        String name
    );
}