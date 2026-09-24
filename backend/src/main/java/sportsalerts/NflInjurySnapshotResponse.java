package sportsalerts;

import java.time.LocalDateTime;
import java.util.List;

public record NflInjurySnapshotResponse(

    String playerName,

    String playerProviderId,

    String headshotUrl,

    String position,

    Integer seasonYear,

    String seasonType,

    Integer weekNumber,

    String injury,

    String secondaryInjury,

    String gameStatus,

    String statusDate,

    String estimatedReturnDate,

    List<NflInjuryPracticeDayResponse> practiceReports,

    LocalDateTime updatedAt

) {
}