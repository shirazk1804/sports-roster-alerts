package sportsalerts;

public record MlbLineupPlayerResponse(

    Long playerId,

    String playerName,

    String position,

    Integer battingOrder

) {
}