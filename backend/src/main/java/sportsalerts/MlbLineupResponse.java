package sportsalerts;

import java.util.List;

public record MlbLineupResponse(

    String state,

    Long gamePk,

    String gameDate,

    String gameStatus,

    String opponentName,

    String homeAway,

    List<MlbLineupPlayerResponse> lineup,

    MlbLineupPlayerResponse startingPitcher

) {
}