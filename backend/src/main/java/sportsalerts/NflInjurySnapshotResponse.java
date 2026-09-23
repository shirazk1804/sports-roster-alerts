package sportsalerts;

import java.time.LocalDateTime;

public record NflInjurySnapshotResponse(
    String playerName,
    String injury,
    String secondaryInjury,
    String gameStatus,
    String practiceStatus,
    String statusDate,
    String estimatedReturnDate,
    LocalDateTime updatedAt
) {
}