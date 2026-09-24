package sportsalerts;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NflInjuryPracticeReportService {

    /*
     * NFL injury reports are based around
     * Eastern Time.
     */
    private static final ZoneId NFL_TIME_ZONE =
        ZoneId.of(
            "America/New_York"
        );

    private final NflInjuryPracticeReportRepository
        reportRepository;

    public NflInjuryPracticeReportService(
        NflInjuryPracticeReportRepository reportRepository
    ) {
        this.reportRepository =
            reportRepository;
    }

    @Transactional
    public int captureReports(
        String externalProviderTeamId,
        NflWeekInfo week,
        List<NflInjuryEvent> injuries
    ) {

        /*
         * This represents the day on which
         * our backend observed this version
         * of the weekly practice report.
         *
         * Do not use injury.statusDate here.
         * Sportradar's status_date represents
         * an injury-status update timestamp,
         * not a dedicated practice-report date.
         */
        LocalDate reportDate =
            LocalDate.now(
                NFL_TIME_ZONE
            );

        int savedOrUpdated = 0;

        for (
            NflInjuryEvent injury :
            injuries
        ) {

            /*
             * A player without a practice status
             * should not create a practice-day
             * entry.
             */
            if (
                injury.practiceStatus() == null ||
                injury.practiceStatus()
                    .isBlank()
            ) {
                continue;
            }

            Optional<NflInjuryPracticeReport>
                existingReport =
                    reportRepository
                        .findByExternalProviderTeamIdAndPlayerProviderIdAndSeasonYearAndSeasonTypeAndWeekNumberAndReportDate(
                            externalProviderTeamId,
                            injury.playerProviderId(),
                            week.seasonYear(),
                            week.seasonType(),
                            week.week(),
                            reportDate
                        );

            if (existingReport.isPresent()) {

                /*
                 * The scheduler runs repeatedly
                 * throughout the day.
                 *
                 * If Sportradar changes today's
                 * report, update today's row
                 * instead of creating another one.
                 */
                NflInjuryPracticeReport report =
                    existingReport.get();

                report.update(
                    injury.teamName(),
                    injury.playerName(),
                    injury.position(),
                    injury.injury(),
                    injury.secondaryInjury(),
                    injury.gameStatus(),
                    injury.practiceStatus(),
                    injury.statusDate()
                );

                reportRepository.save(
                    report
                );

            } else {

                /*
                 * First practice-report observation
                 * for this player on this day.
                 */
                NflInjuryPracticeReport report =
                    new NflInjuryPracticeReport(
                        externalProviderTeamId,
                        injury.teamName(),
                        injury.playerProviderId(),
                        injury.playerName(),
                        injury.position(),
                        week.seasonYear(),
                        week.seasonType(),
                        week.week(),
                        reportDate,
                        injury.injury(),
                        injury.secondaryInjury(),
                        injury.gameStatus(),
                        injury.practiceStatus(),
                        injury.statusDate()
                    );

                reportRepository.save(
                    report
                );
            }

            savedOrUpdated++;
        }

        return savedOrUpdated;
    }

    public List<NflInjuryPracticeReport>
        getWeeklyReports(
            String externalProviderTeamId,
            NflWeekInfo week
        ) {

        return reportRepository
            .findByExternalProviderTeamIdAndSeasonYearAndSeasonTypeAndWeekNumberOrderByPlayerNameAscReportDateAsc(
                externalProviderTeamId,
                week.seasonYear(),
                week.seasonType(),
                week.week()
            );
    }
}