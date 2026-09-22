package sportsalerts;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class RosterEventController {

    private final RosterEventService
        rosterEventService;

    public RosterEventController(
        RosterEventService rosterEventService
    ) {
        this.rosterEventService =
            rosterEventService;
    }

    @GetMapping
    public List<RosterEvent> getEvents() {
        return rosterEventService
            .getVisibleEvents();
    }
}