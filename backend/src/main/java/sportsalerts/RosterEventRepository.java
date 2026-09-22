package sportsalerts;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RosterEventRepository
    extends JpaRepository<RosterEvent, Long> {

    boolean existsByDedupeKey(
        String dedupeKey
    );

    List<RosterEvent>
        findAllByOrderByEventDateDescIdDesc();
}