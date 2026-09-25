package sportsalerts;

public record NbaInjuryEvent(
        String externalProviderTeamId,
        String teamName,
        String playerProviderId,
        String playerName,
        String position,
        String injuryProviderId,
        String injury,
        String status,
        String comment,
        String startDate,
        String updateDate) {
}