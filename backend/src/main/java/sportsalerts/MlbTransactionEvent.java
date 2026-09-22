package sportsalerts;

public record MlbTransactionEvent(
    Long sourceTransactionId,
    Long playerId,
    String playerName,
    String teamName,
    String eventType,
    String date,
    String description
) {
}