package sportsalerts;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface FollowedTeamRepository
    extends JpaRepository<FollowedTeam, Long> {

    List<FollowedTeam> findByAppUserId(
        Long appUserId
    );

    boolean existsByAppUserIdAndLeagueAndName(
        Long appUserId,
        String league,
        String name
    );

    Optional<FollowedTeam>
        findByAppUserIdAndLeagueAndName(
            Long appUserId,
            String league,
            String name
        );
}