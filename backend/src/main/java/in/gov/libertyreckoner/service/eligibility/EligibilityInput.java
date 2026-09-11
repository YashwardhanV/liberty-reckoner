package in.gov.libertyreckoner.service.eligibility;

import java.time.LocalDate;
import java.util.List;

public record EligibilityInput(
        int previousConvictions,
        boolean convictionHistoryVerified,
        int activeCaseCount,
        int accusedDelayDays,
        boolean accusedDelayVerified,
        LocalDate bailGrantedDate,
        LocalDate physicalReleaseDate,
        List<ChargeInput> charges,
        List<CustodyInput> custodyPeriods) {

    public record ChargeInput(Integer maximumTermDays, boolean active,
                              boolean deathPunishmentPossible, boolean lifeImprisonmentPossible,
                              String citation) { }

    public record CustodyInput(LocalDate startDate, LocalDate endDate, boolean included, boolean verified) { }
}

