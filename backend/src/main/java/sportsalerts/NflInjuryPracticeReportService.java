package sportsalerts;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NflInjuryPracticeReportService {

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

        int savedOrUpdated = 0;

        for (
            NflInjuryEvent injury :
            injuries
        ) {

            /*
             * Only save an actual practice
             * participation report.
             */
            if (
                injury.practiceStatus() == null ||
                injury.practiceStatus()
                    .isBlank()
            ) {
                continue;
            }

            /*
             * Use Sportradar's status date
             * instead of the current server date.
             *
             * This prevents Wednesday's report
             * from becoming Thursday simply
             * because it is already past midnight
             * on the East Coast.
             */
            LocalDate reportDate =
                resolveReportDate(
                    injury
                );

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
                 * If Sportradar changes the
                 * report for this same day,
                 * update the existing row.
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
                 * First practice report for
                 * this player on this date.
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

    private LocalDate resolveReportDate(
        NflInjuryEvent injury
    ) {

        String statusDate =
            injury.statusDate();

        if (
            statusDate != null &&
            statusDate.length() >= 10
        ) {
            try {
                return LocalDate.parse(
                    statusDate.substring(
                        0,
                        10
                    )
                );

            } catch (Exception ignored) {
            }
        }

        /*
         * Fallback only if Sportradar does
         * not provide a usable status date.
         */
        return LocalDate.now(
            ZoneOffset.UTC
        );
    }
}