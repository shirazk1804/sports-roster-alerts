package sportsalerts;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/events")
public class RosterEventController {

    public record RosterEventResponse(
            Long id,
            String league,
            String teamName,
            String playerName,
            String eventType,
            LocalDate eventDate,
            String description,
            LocalDateTime createdAt) {
    }

    private final RosterEventService
            rosterEventService;

    private final AppUserService
            appUserService;

    public RosterEventController(
            RosterEventService rosterEventService,
            AppUserService appUserService) {

        this.rosterEventService =
                rosterEventService;

        this.appUserService =
                appUserService;
    }

    @GetMapping
    public List<RosterEventResponse> getEvents(
            @RequestHeader(
                    value = "Authorization",
                    required = false)
            String authorizationHeader) {

        AppUser user =
                appUserService
                        .requireAuthenticatedUser(
                                authorizationHeader);

        return rosterEventService
                .getVisibleEventsForUser(
                        user.getId())
                .stream()
                .map(event ->
                        new RosterEventResponse(
                                event.getId(),
                                event.getLeague(),
                                event.getTeamName(),
                                event.getPlayerName(),
                                event.getEventType(),
                                event.getEventDate(),
                                event.getDescription(),
                                event.getCreatedAt()))
                .toList();
    }
}