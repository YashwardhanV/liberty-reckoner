package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.api.ApiDtos.ApplicationPacketView;
import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service @RequiredArgsConstructor
public class ApplicationPacketService {
    private final PrisonerCaseRepository prisonerCases;
    private final LegalAssessmentRepository assessments;

    @Transactional(readOnly = true)
    public ApplicationPacketView generate(UUID caseId) {
        PrisonerCase pc = prisonerCases.findDetailedById(caseId)
                .orElseThrow(() -> new ResourceNotFoundException("Prisoner case not found"));
        LegalAssessment assessment = assessments.findFirstByPrisonerCaseIdAndCurrentTrueOrderByAssessedAtDesc(caseId)
                .orElseThrow(() -> new BusinessRuleException("Run an eligibility assessment before generating an application"));
        if (!EnumSet.of(EligibilityStatus.ACTION_OVERDUE, EligibilityStatus.MAXIMUM_REACHED,
                EligibilityStatus.APPLICATION_FILED).contains(assessment.getStatus())) {
            throw new BusinessRuleException("The current assessment does not support a Section 479 application packet");
        }

        Prisoner prisoner = pc.getPrisoner();
        LegalCase legalCase = pc.getLegalCase();
        String threshold = assessment.getProjectedThresholdDate() == null ? "recorded threshold"
                : assessment.getProjectedThresholdDate().format(DateTimeFormatter.ofPattern("dd MMMM uuuu"));
        List<String> facts = List.of(
                "Prisoner: " + prisoner.getFullName() + " (" + prisoner.getPrisonNumber() + ")",
                "Case: " + legalCase.getCnrNumber() + "; FIR " + legalCase.getFirNumber(),
                "Current stage: " + legalCase.getStage(),
                "Credited custody: " + assessment.getCreditedCustodyDays() + " days",
                "Excluded accused-caused delay: " + assessment.getExcludedDelayDays() + " days",
                "Applicable threshold: " + assessment.getThresholdFraction() + " — " + threshold,
                "Rule set: " + assessment.getRuleVersion());
        String body = "The above-named undertrial has reached the custody threshold calculated under Section 479 " +
                "of the Bharatiya Nagarik Suraksha Sanhita, 2023. In discharge of the duty under sub-section (3), " +
                "the Jail Superintendent respectfully requests the Court to proceed under sub-section (1). " +
                "The attached computation is based on verified custody periods and the currently recorded charge. " +
                "The Court may verify the source records and pass an appropriate order in accordance with law.";
        String actor = SecurityContextHolder.getContext().getAuthentication().getName();
        return new ApplicationPacketView(pc.getId(), "NP-479-" + pc.getId().toString().substring(0, 8).toUpperCase(),
                "Application under Section 479(3), BNSS 2023", "The Presiding Officer, " + legalCase.getCourtName(),
                "Release consideration for " + prisoner.getFullName() + " — CNR " + legalCase.getCnrNumber(),
                facts, body, "Generated from verified Liberty Reckoner records; Superintendent signature and legal review required.",
                actor, Instant.now());
    }
}

