package in.gov.libertyreckoner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.libertyreckoner.api.ApiDtos.AssessmentView;
import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.repository.*;
import in.gov.libertyreckoner.service.eligibility.*;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class EligibilityService {
    private final PrisonerCaseRepository prisonerCases;
    private final LegalAssessmentRepository assessments;
    private final WorkflowTaskRepository tasks;
    private final EligibilityEngine engine;
    private final ObjectMapper objectMapper;
    private final ApiMapper mapper;
    private final AuditService auditService;
    private final NotificationOutboxService notificationOutbox;
    private final MeterRegistry meters;

    @Transactional
    public AssessmentView evaluate(UUID prisonerCaseId) {
        PrisonerCase pc = prisonerCases.findForEvaluationById(prisonerCaseId)
                .orElseThrow(() -> new ResourceNotFoundException("Prisoner case not found"));
        int activeCaseCount = Math.toIntExact(prisonerCases.countByPrisonerIdAndActiveTrue(pc.getPrisoner().getId()));
        EligibilityDecision decision = engine.evaluate(toInput(pc, activeCaseCount), LocalDate.now());

        EligibilityStatus status = decision.status();
        LegalAssessment previous = assessments.findFirstByPrisonerCaseIdAndCurrentTrueOrderByAssessedAtDesc(pc.getId()).orElse(null);
        EligibilityStatus previousStatus = previous == null ? null : previous.getStatus();
        if (previous != null && previous.getStatus() == EligibilityStatus.APPLICATION_FILED
                && status == EligibilityStatus.ACTION_OVERDUE) status = EligibilityStatus.APPLICATION_FILED;

        assessments.markExistingAssessmentsHistorical(pc.getId());
        LegalAssessment saved;
        try {
            saved = assessments.save(LegalAssessment.builder()
                    .prisonerCase(pc).status(status).verificationStatus(decision.verificationStatus())
                    .maximumTermDays(decision.maximumTermDays()).thresholdDays(decision.thresholdDays())
                    .creditedCustodyDays(decision.creditedCustodyDays()).excludedDelayDays(decision.excludedDelayDays())
                    .projectedThresholdDate(decision.projectedThresholdDate()).daysRemaining(decision.daysRemaining())
                    .thresholdFraction(decision.thresholdFraction()).ruleVersion(EligibilityEngine.RULE_VERSION)
                    .explanation(decision.explanation()).blockersJson(objectMapper.writeValueAsString(decision.blockers()))
                    .build());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not store eligibility assessment", exception);
        }
        if (isStatutoryAlert(status) && previousStatus != status) {
            notificationOutbox.enqueueEligibilityAlerts(saved);
        }
        ensureWorkflow(pc, status, decision.projectedThresholdDate());
        meters.counter("libertyreckoner.eligibility.evaluations", "status", status.name()).increment();
        auditService.record("PRISONER_CASE", pc.getId(), "ELIGIBILITY_EVALUATED",
                Map.of("status", status.name(), "ruleVersion", EligibilityEngine.RULE_VERSION,
                        "creditedCustodyDays", decision.creditedCustodyDays()));
        return mapper.assessment(saved);
    }

    @Transactional(readOnly = true)
    public LegalAssessment currentEntity(UUID caseId) {
        return assessments.findFirstByPrisonerCaseIdAndCurrentTrueOrderByAssessedAtDesc(caseId).orElse(null);
    }

    private EligibilityInput toInput(PrisonerCase pc, int activeCaseCount) {
        List<EligibilityInput.ChargeInput> charges = pc.getCharges().stream()
                .map(c -> new EligibilityInput.ChargeInput(c.getMaximumTermDays(), c.isActive(),
                        c.isDeathPunishmentPossible(), c.isLifeImprisonmentPossible(),
                        c.getActName() + " " + c.getSectionCode())).toList();
        List<EligibilityInput.CustodyInput> custody = pc.getCustodyPeriods().stream()
                .map(c -> new EligibilityInput.CustodyInput(c.getStartDate(), c.getEndDate(),
                        c.isIncluded(), c.isVerified())).toList();
        Prisoner prisoner = pc.getPrisoner();
        return new EligibilityInput(prisoner.getPreviousConvictions(), prisoner.isConvictionHistoryVerified(),
                activeCaseCount, pc.getAccusedDelayDays(), pc.isAccusedDelayVerified(), pc.getBailGrantedDate(),
                pc.getPhysicalReleaseDate(), charges, custody);
    }

    private void ensureWorkflow(PrisonerCase pc, EligibilityStatus status, LocalDate thresholdDate) {
        if (tasks.existsByPrisonerCaseIdAndStatusIn(pc.getId(), List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS))) return;
        WorkflowTask task = switch (status) {
            case ACTION_OVERDUE, MAXIMUM_REACHED -> WorkflowTask.builder().prisonerCase(pc)
                    .type(TaskType.PREPARE_APPLICATION).assignedRole(UserRole.SUPERINTENDENT)
                    .title("Prepare and verify statutory release application")
                    .dueAt(Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS)).build();
            case DUE_SOON -> WorkflowTask.builder().prisonerCase(pc).type(TaskType.VERIFY_RECORDS)
                    .assignedRole(UserRole.DLSA_COUNSEL).title("Verify records before threshold date")
                    .dueAt((thresholdDate == null ? LocalDate.now().plusDays(30) : thresholdDate.minusDays(30))
                            .atStartOfDay(ZoneId.of("Asia/Kolkata")).toInstant()).build();
            case DATA_INCOMPLETE -> WorkflowTask.builder().prisonerCase(pc).type(TaskType.RESOLVE_DATA_CONFLICT)
                    .assignedRole(UserRole.SUPERINTENDENT).title("Resolve missing or contradictory custody data")
                    .dueAt(Instant.now().plus(3, java.time.temporal.ChronoUnit.DAYS)).build();
            case LEGAL_REVIEW, SECTION_479_INAPPLICABLE -> WorkflowTask.builder().prisonerCase(pc)
                    .type(TaskType.VERIFY_RECORDS).assignedRole(UserRole.DLSA_COUNSEL)
                    .title("Review statutory restriction and alternate bail grounds")
                    .dueAt(Instant.now().plus(7, java.time.temporal.ChronoUnit.DAYS)).build();
            case BAIL_GRANTED -> WorkflowTask.builder().prisonerCase(pc).type(TaskType.SATISFY_BOND)
                    .assignedRole(UserRole.DLSA_COUNSEL).title("Complete bond requirements and remove release barriers")
                    .dueAt(Instant.now().plus(1, java.time.temporal.ChronoUnit.DAYS)).build();
            default -> null;
        };
        if (task != null) tasks.save(task);
    }

    private boolean isStatutoryAlert(EligibilityStatus status) {
        return status == EligibilityStatus.ACTION_OVERDUE || status == EligibilityStatus.MAXIMUM_REACHED;
    }
}
