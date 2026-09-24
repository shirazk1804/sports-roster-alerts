package sportsalerts;

public record NflInjuryEvent(

    String externalProviderTeamId,

    String teamName,

    String playerProviderId,

    String playerName,

    String position,

    String injury,

    String secondaryInjury,

    String gameStatus,

    String practiceStatus,

    String statusDate,

    String estimatedReturnDate,

    String description

) {

    /*
     * Keeps older tests/code working while
     * we migrate to storing player position.
     */
    public NflInjuryEvent(
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
        this(
            externalProviderTeamId,
            teamName,
            playerProviderId,
            playerName,
            "",
            injury,
            secondaryInjury,
            gameStatus,
            practiceStatus,
            statusDate,
            estimatedReturnDate,
            description
        );
    }
}