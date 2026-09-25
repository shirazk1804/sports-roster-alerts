package sportsalerts;

public record NbaTransactionEvent(
        String sourceProviderEventId,
        String playerProviderId,
        String playerName,
        String externalProviderTeamId,
        String teamName,
        String eventType,
        String effectiveDate,
        String description,
        String transactionCode,
        String transactionType,
        String notes) {
}