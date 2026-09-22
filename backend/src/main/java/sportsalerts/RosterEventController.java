package sportsalerts;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class RosterEventController {

    private final RosterEventService
        rosterEventService;

    private final AppUserService
        appUserService;

    public RosterEventController(
        RosterEventService rosterEventService,
        AppUserService appUserService
    ) {
        this.rosterEventService =
            rosterEventService;

        this.appUserService =
            appUserService;
    }

    @GetMapping
    public List<RosterEvent> getEvents(
        @RequestParam String installationId
    ) {
        AppUser user =
            appUserService.getOrCreateUser(
                installationId
            );

        return rosterEventService
            .getVisibleEventsForUser(
                user.getId()
            );
    }
}