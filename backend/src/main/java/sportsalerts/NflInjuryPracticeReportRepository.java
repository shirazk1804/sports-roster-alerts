package sportsalerts;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NflInjuryPracticeReportRepository
    extends JpaRepository<
        NflInjuryPracticeReport,
        Long
    > {

    Optional<NflInjuryPracticeReport>
        findByExternalProviderTeamIdAndPlayerProviderIdAndSeasonYearAndSeasonTypeAndWeekNumberAndReportDate(
            String externalProviderTeamId,
            String playerProviderId,
            Integer seasonYear,
            String seasonType,
            Integer weekNumber,
            LocalDate reportDate
        );

    List<NflInjuryPracticeReport>
        findByExternalProviderTeamIdAndSeasonYearAndSeasonTypeAndWeekNumberOrderByPlayerNameAscReportDateAsc(
            String externalProviderTeamId,
            Integer seasonYear,
            String seasonType,
            Integer weekNumber
        );
}