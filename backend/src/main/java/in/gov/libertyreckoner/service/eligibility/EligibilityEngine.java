package in.gov.libertyreckoner.service.eligibility;

import in.gov.libertyreckoner.domain.EligibilityStatus;
import in.gov.libertyreckoner.domain.VerificationStatus;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Component
public class EligibilityEngine {
    public static final String RULE_VERSION = "BNSS-479-v1.0";

    public EligibilityDecision evaluate(EligibilityInput input, LocalDate asOfDate) {
        List<String> blockers = new ArrayList<>();
        List<EligibilityInput.ChargeInput> activeCharges = input.charges().stream()
                .filter(EligibilityInput.ChargeInput::active).toList();

        int creditedDays = mergedCustodyDays(input.custodyPeriods(), asOfDate);
        int excludedDelay = input.accusedDelayVerified() ? input.accusedDelayDays() : 0;
        creditedDays = Math.max(0, creditedDays - excludedDelay);

        if (input.physicalReleaseDate() != null) {
            return decision(EligibilityStatus.RELEASED, VerificationStatus.VERIFIED, null, null,
                    creditedDays, excludedDelay, null, 0, "Not applicable",
                    "Physical release has been recorded.", blockers);
        }
        if (input.bailGrantedDate() != null) {
            return decision(EligibilityStatus.BAIL_GRANTED, VerificationStatus.VERIFIED, null, null,
                    creditedDays, excludedDelay, null, 0, "Order granted",
                    "A bail order is recorded; release execution and other lawful holds must be verified.", blockers);
        }

        if (!activeCharges.isEmpty()
                && activeCharges.stream().anyMatch(c -> c.deathPunishmentPossible() || c.lifeImprisonmentPossible())) {
            blockers.add("Death or life imprisonment is specified as a possible punishment for an active charge.");
            return decision(EligibilityStatus.SECTION_479_INAPPLICABLE, VerificationStatus.VERIFIED,
                    null, null, creditedDays, excludedDelay, null, null, "Excluded",
                    "The Section 479 threshold benefit is inapplicable; other bail grounds require legal review.", blockers);
        }

        boolean unverifiedCustody = input.custodyPeriods().stream()
                .anyMatch(period -> period.included() && !period.verified());
        if (!input.convictionHistoryVerified()) blockers.add("Nationwide conviction history is not verified.");
        if (!input.accusedDelayVerified()) blockers.add("Accused-caused delay exclusions are not verified.");
        if (unverifiedCustody) blockers.add("One or more included custody periods are unverified.");
        if (activeCharges.isEmpty()) blockers.add("No active charge is available for assessment.");
        if (activeCharges.stream().anyMatch(charge -> charge.maximumTermDays() == null))
            blockers.add("At least one active charge has no approved maximum term.");

        if (!blockers.isEmpty()) {
            return decision(EligibilityStatus.DATA_INCOMPLETE, VerificationStatus.NEEDS_REVIEW, null, null,
                    creditedDays, excludedDelay, null, null, "Undetermined",
                    "The calculation is paused because an authoritative input is missing or unverified.", blockers);
        }

        int maximumTermDays = activeCharges.stream().map(EligibilityInput.ChargeInput::maximumTermDays)
                .filter(Objects::nonNull).max(Integer::compareTo).orElseThrow();
        int divisor = input.previousConvictions() == 0 ? 3 : 2;
        int thresholdDays = (int) Math.ceil(maximumTermDays / (double) divisor);
        String fraction = divisor == 3 ? "One-third" : "One-half";
        int remaining = thresholdDays - creditedDays;
        LocalDate projectedDate = asOfDate.plusDays(remaining);

        if (creditedDays >= maximumTermDays) {
            blockers.add("Credited detention has reached the recorded maximum imprisonment term.");
            return decision(EligibilityStatus.MAXIMUM_REACHED, VerificationStatus.VERIFIED, maximumTermDays,
                    thresholdDays, creditedDays, excludedDelay, projectedDate, remaining, fraction,
                    "Urgent court and custody review is required under the maximum-period safeguard.", blockers);
        }

        if (activeCharges.size() > 1) blockers.add("More than one active offence is pending.");
        if (input.activeCaseCount() > 1) blockers.add("Multiple active cases are linked to this person.");
        if (!blockers.isEmpty()) {
            return decision(EligibilityStatus.LEGAL_REVIEW, VerificationStatus.VERIFIED, maximumTermDays,
                    thresholdDays, creditedDays, excludedDelay, projectedDate, remaining, fraction,
                    "The threshold is shown for legal review, but the multiple-offence or multiple-case restriction is engaged.", blockers);
        }

        if (remaining <= 0) {
            return decision(EligibilityStatus.ACTION_OVERDUE, VerificationStatus.VERIFIED, maximumTermDays,
                    thresholdDays, creditedDays, excludedDelay, projectedDate, remaining, fraction,
                    "The statutory custody threshold has been crossed. The Superintendent application workflow is due.", blockers);
        }
        if (remaining <= 90) {
            return decision(EligibilityStatus.DUE_SOON, VerificationStatus.VERIFIED, maximumTermDays,
                    thresholdDays, creditedDays, excludedDelay, projectedDate, remaining, fraction,
                    "The threshold is approaching; records and the draft application should be prepared now.", blockers);
        }
        return decision(EligibilityStatus.NOT_DUE, VerificationStatus.VERIFIED, maximumTermDays,
                thresholdDays, creditedDays, excludedDelay, projectedDate, remaining, fraction,
                "The verified credited custody period has not yet reached the applicable threshold.", blockers);
    }

    private EligibilityDecision decision(EligibilityStatus status, VerificationStatus verification,
            Integer maximumTerm, Integer threshold, int custody, int excluded, LocalDate date,
            Integer remaining, String fraction, String explanation, List<String> blockers) {
        return new EligibilityDecision(status, verification, maximumTerm, threshold, custody, excluded,
                date, remaining, fraction, explanation, List.copyOf(blockers));
    }

    private int mergedCustodyDays(List<EligibilityInput.CustodyInput> periods, LocalDate asOfDate) {
        List<DateRange> ranges = periods.stream()
                .filter(EligibilityInput.CustodyInput::included)
                .filter(EligibilityInput.CustodyInput::verified)
                .map(period -> new DateRange(period.startDate(),
                        period.endDate() == null || period.endDate().isAfter(asOfDate) ? asOfDate : period.endDate()))
                .filter(range -> !range.end().isBefore(range.start()))
                .sorted(Comparator.comparing(DateRange::start))
                .toList();
        if (ranges.isEmpty()) return 0;

        int total = 0;
        LocalDate start = ranges.getFirst().start();
        LocalDate end = ranges.getFirst().end();
        for (int i = 1; i < ranges.size(); i++) {
            DateRange next = ranges.get(i);
            if (!next.start().isAfter(end.plusDays(1))) {
                if (next.end().isAfter(end)) end = next.end();
            } else {
                total += Math.toIntExact(ChronoUnit.DAYS.between(start, end) + 1);
                start = next.start();
                end = next.end();
            }
        }
        return total + Math.toIntExact(ChronoUnit.DAYS.between(start, end) + 1);
    }

    private record DateRange(LocalDate start, LocalDate end) { }
}
