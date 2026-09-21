package in.gov.libertyreckoner.service.eligibility;

import in.gov.libertyreckoner.domain.EligibilityStatus;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.assertj.core.api.Assertions.assertThat;

class EligibilityEngineTest {
    private final EligibilityEngine engine = new EligibilityEngine();
    private final LocalDate today = LocalDate.of(2026, 8, 10);

    @Test
    void firstTimeOffenderCrossesOneThirdThreshold() {
        var decision = engine.evaluate(input(0, true, 1, 0, true,
                List.of(charge(1095, false, false)), List.of(custody(400, true))), today);

        assertThat(decision.status()).isEqualTo(EligibilityStatus.ACTION_OVERDUE);
        assertThat(decision.thresholdDays()).isEqualTo(365);
        assertThat(decision.creditedCustodyDays()).isEqualTo(400);
        assertThat(decision.thresholdFraction()).isEqualTo("One-third");
    }

    @Test
    void previousConvictionUsesOneHalfThreshold() {
        var decision = engine.evaluate(input(1, true, 1, 0, true,
                List.of(charge(1095, false, false)), List.of(custody(400, true))), today);

        assertThat(decision.status()).isEqualTo(EligibilityStatus.NOT_DUE);
        assertThat(decision.thresholdDays()).isEqualTo(548);
        assertThat(decision.thresholdFraction()).isEqualTo("One-half");
    }

    @Test
    void multipleActiveCasesRequireLegalReview() {
        var decision = engine.evaluate(input(0, true, 2, 0, true,
                List.of(charge(1095, false, false)), List.of(custody(400, true))), today);

        assertThat(decision.status()).isEqualTo(EligibilityStatus.LEGAL_REVIEW);
        assertThat(decision.blockers()).anyMatch(value -> value.contains("Multiple active cases"));
    }

    @Test
    void lifePunishmentExclusionDoesNotRequireFiniteMaximum() {
        var decision = engine.evaluate(input(0, false, 1, 0, true,
                List.of(charge(null, false, true)), List.of(custody(400, true))), today);

        assertThat(decision.status()).isEqualTo(EligibilityStatus.SECTION_479_INAPPLICABLE);
        assertThat(decision.thresholdDays()).isNull();
    }

    @Test
    void missingConvictionVerificationPausesCalculation() {
        var decision = engine.evaluate(input(0, false, 1, 0, true,
                List.of(charge(1095, false, false)), List.of(custody(400, true))), today);

        assertThat(decision.status()).isEqualTo(EligibilityStatus.DATA_INCOMPLETE);
        assertThat(decision.blockers()).anyMatch(value -> value.contains("conviction history"));
    }

    @Test
    void overlappingCustodyPeriodsAreNotDoubleCounted() {
        var first = new EligibilityInput.CustodyInput(today.minusDays(99), today.minusDays(25), true, true);
        var overlapping = new EligibilityInput.CustodyInput(today.minusDays(49), null, true, true);
        var decision = engine.evaluate(input(0, true, 1, 0, true,
                List.of(charge(1095, false, false)), List.of(first, overlapping)), today);

        assertThat(decision.creditedCustodyDays()).isEqualTo(100);
    }

    @Test
    void verifiedDelayIsExcludedAndMaximumSafeguardWins() {
        var decision = engine.evaluate(input(0, true, 2, 5, true,
                List.of(charge(365, false, false)), List.of(custody(380, true))), today);

        assertThat(decision.creditedCustodyDays()).isEqualTo(375);
        assertThat(decision.status()).isEqualTo(EligibilityStatus.MAXIMUM_REACHED);
    }

    private EligibilityInput input(int convictions, boolean convictionVerified, int activeCases,
            int delayDays, boolean delayVerified, List<EligibilityInput.ChargeInput> charges,
            List<EligibilityInput.CustodyInput> custody) {
        return new EligibilityInput(convictions, convictionVerified, activeCases, delayDays, delayVerified,
                null, null, charges, custody);
    }

    private EligibilityInput.ChargeInput charge(Integer maxDays, boolean death, boolean life) {
        return new EligibilityInput.ChargeInput(maxDays, true, death, life, "Test section");
    }

    private EligibilityInput.CustodyInput custody(int days, boolean verified) {
        return new EligibilityInput.CustodyInput(today.minusDays(days - 1L), null, true, verified);
    }
}
