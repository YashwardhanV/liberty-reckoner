package in.gov.libertyreckoner.service.eligibility;

import in.gov.libertyreckoner.domain.EligibilityStatus;
import in.gov.libertyreckoner.domain.VerificationStatus;
import java.time.LocalDate;
import java.util.List;

public record EligibilityDecision(
        EligibilityStatus status,
        VerificationStatus verificationStatus,
        Integer maximumTermDays,
        Integer thresholdDays,
        int creditedCustodyDays,
        int excludedDelayDays,
        LocalDate projectedThresholdDate,
        Integer daysRemaining,
        String thresholdFraction,
        String explanation,
        List<String> blockers) { }

