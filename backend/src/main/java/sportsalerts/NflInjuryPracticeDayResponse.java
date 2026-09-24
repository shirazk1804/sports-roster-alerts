package sportsalerts;

import java.time.LocalDate;

public record NflInjuryPracticeDayResponse(

    LocalDate reportDate,

    String practiceStatus

) {
}