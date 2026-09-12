package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.repository.PrisonerCaseRepository;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EligibilityRefreshJob {
    private final PrisonerCaseRepository prisonerCases;
    private final EligibilityService eligibilityService;
    private final MeterRegistry meters;

    @Scheduled(cron = "${libertyreckoner.eligibility.refresh-cron}",
            zone = "${libertyreckoner.eligibility.refresh-zone}")
    public void refreshAllActiveCases() {
        for (UUID caseId : prisonerCases.findActiveIds()) {
            try {
                eligibilityService.evaluate(caseId);
                meters.counter("libertyreckoner.eligibility.refresh", "outcome", "success").increment();
            } catch (Exception exception) {
                log.error("Scheduled eligibility refresh failed [caseId={}]", caseId, exception);
                meters.counter("libertyreckoner.eligibility.refresh", "outcome", "failed").increment();
            }
        }
    }
}
