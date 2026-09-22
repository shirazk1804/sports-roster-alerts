package sportsalerts;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertPreferenceRepository
    extends JpaRepository<AlertPreference, Long> {

    List<AlertPreference>
        findByFollowedTeamId(Long followedTeamId);

    void deleteByFollowedTeamId(
        Long followedTeamId
    );
}