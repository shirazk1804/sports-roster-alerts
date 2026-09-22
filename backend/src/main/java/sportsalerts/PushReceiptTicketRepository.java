package sportsalerts;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PushReceiptTicketRepository
    extends JpaRepository<PushReceiptTicket, Long> {

    List<PushReceiptTicket>
        findTop1000ByCreatedAtBeforeOrderByCreatedAtAsc(
            LocalDateTime cutoff
        );
}