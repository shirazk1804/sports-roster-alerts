package sportsalerts;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowedTeamRepository
    extends JpaRepository<FollowedTeam, Long> {

    boolean existsByLeagueAndName(
        String league,
        String name
    );
}