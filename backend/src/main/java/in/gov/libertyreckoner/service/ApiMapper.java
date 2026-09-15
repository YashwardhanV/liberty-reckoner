package in.gov.libertyreckoner.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.domain.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Component @RequiredArgsConstructor
public class ApiMapper {
    private final ObjectMapper objectMapper;

    public AssessmentView assessment(LegalAssessment value) {
        if (value == null) return null;
        List<String> blockers;
        try {
            blockers = objectMapper.readValue(value.getBlockersJson(), new TypeReference<>() {});
        } catch (Exception ignored) {
            blockers = Collections.emptyList();
        }
        return new AssessmentView(value.getId(), value.getStatus(), value.getVerificationStatus(),
                value.getMaximumTermDays(), value.getThresholdDays(), value.getCreditedCustodyDays(),
                value.getExcludedDelayDays(), value.getProjectedThresholdDate(), value.getDaysRemaining(),
                value.getThresholdFraction(), value.getRuleVersion(), value.getExplanation(), blockers,
                value.getAssessedAt());
    }

    public TaskView task(WorkflowTask task) {
        PrisonerCase pc = task.getPrisonerCase();
        return new TaskView(task.getId(), pc.getPrisoner().getId(), pc.getId(), pc.getPrisoner().getFullName(),
                pc.getPrisoner().getPrisonNumber(), pc.getLegalCase().getCnrNumber(), task.getType(),
                task.getAssignedRole(), task.getStatus(), task.getTitle(), task.getNotes(), task.getDueAt(),
                task.getCompletedAt(), task.getCreatedAt());
    }

    public PrisonerCaseView prisonerCase(PrisonerCase pc, LegalAssessment assessment) {
        LegalCase legalCase = pc.getLegalCase();
        List<ChargeView> charges = pc.getCharges().stream().filter(Charge::isActive)
                .map(c -> new ChargeView(c.getId(), c.getActName(), c.getSectionCode(), c.getDescription(),
                        c.getMaximumTermDays(), c.getMaximumTermLabel(), c.isDeathPunishmentPossible(),
                        c.isLifeImprisonmentPossible(), c.getEffectiveFrom(), c.getLegalSource())).toList();
        List<CustodyView> custody = pc.getCustodyPeriods().stream()
                .sorted(Comparator.comparing(CustodyPeriod::getStartDate))
                .map(c -> new CustodyView(c.getId(), c.getStartDate(), c.getEndDate(), c.isIncluded(),
                        c.getExclusionReason(), c.getSourceSystem(), c.isVerified())).toList();
        List<TaskView> tasks = pc.getWorkflowTasks().stream()
                .sorted(Comparator.comparing(WorkflowTask::getCreatedAt)).map(this::task).toList();
        return new PrisonerCaseView(pc.getId(), legalCase.getCnrNumber(), legalCase.getFirNumber(),
                legalCase.getPoliceStation(), legalCase.getCourtName(), legalCase.getStage(),
                legalCase.getChargeSheetDate(), legalCase.getNextHearingDate(), pc.isActive(),
                pc.getAccusedDelayDays(), pc.isAccusedDelayVerified(), pc.getBailGrantedDate(),
                pc.getPhysicalReleaseDate(), charges, custody, assessment(assessment), tasks);
    }
}
