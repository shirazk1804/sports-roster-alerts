package sportsalerts;

public record NflInjuryEvent(
    String externalProviderTeamId,
    String teamName,
    String playerProviderId,
    String playerName,
    String injury,
    String secondaryInjury,
    String gameStatus,
    String practiceStatus,
    String statusDate,
    String estimatedReturnDate,
    String description
) {
}