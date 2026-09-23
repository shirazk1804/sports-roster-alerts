package sportsalerts;

public record NflTransactionEvent(
    String sourceProviderEventId,
    String playerProviderId,
    String playerName,
    String externalProviderTeamId,
    String teamName,
    String eventType,
    String effectiveDate,
    String description,
    String transactionCode,
    String statusBefore,
    String statusAfter
) {
}