package sportsalerts;

public record MlbLineupPlayerResponse(
        Long playerId,
        String fullName,
        String position,
        Integer battingOrder) {
}