package sportsalerts;

import java.time.LocalDateTime;

public record NbaInjurySnapshotResponse(

    String playerName,

    String playerProviderId,

    String position,

    String injury,

    String status,

    String comment,

    String startDate,

    String updateDate,

    LocalDateTime updatedAt

) {
}