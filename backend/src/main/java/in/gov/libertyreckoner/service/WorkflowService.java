package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class WorkflowService {
    private final WorkflowTaskRepository tasks;
    private final PrisonerCaseRepository prisonerCases;
    private final LegalAssessmentRepository assessments;
    private final ApiMapper mapper;
    private final AuditService auditService;

    @Transactional(readOnly = true)
    public List<TaskView> queue() {
        return tasks.findByStatusInOrderByDueAtAsc(List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS))
                .stream().map(mapper::task).toList();
    }

    @Transactional
    public TaskView start(UUID taskId) {
        WorkflowTask task = load(taskId);
        if (task.getStatus() != TaskStatus.OPEN) throw new BusinessRuleException("Only open tasks can be started");
        task.setStatus(TaskStatus.IN_PROGRESS);
        auditService.record("WORKFLOW_TASK", taskId, "TASK_STARTED", Map.of("type", task.getType().name()));
        return mapper.task(tasks.save(task));
    }

    @Transactional
    public TaskView complete(UUID taskId, CompleteTaskRequest request) {
        WorkflowTask task = load(taskId);
        if (task.getStatus() == TaskStatus.COMPLETED || task.getStatus() == TaskStatus.CANCELLED)
            throw new BusinessRuleException("This task is already closed");
        TaskOutcome outcome = request.outcome() == null ? TaskOutcome.PROCEED : request.outcome();
        PrisonerCase pc = task.getPrisonerCase();

        if (task.getType() == TaskType.EXECUTE_RELEASE
                && prisonerCases.countByPrisonerIdAndActiveTrue(pc.getPrisoner().getId()) > 1) {
            throw new BusinessRuleException("Physical release cannot be confirmed while another active case or hold remains");
        }

        task.setStatus(TaskStatus.COMPLETED);
        task.setCompletedAt(Instant.now());
        task.setNotes(request.notes());
        tasks.save(task);
        applyOutcome(task, outcome, request.effectiveDate());
        createNextTask(task, outcome);
        auditService.record("WORKFLOW_TASK", taskId, "TASK_COMPLETED",
                Map.of("type", task.getType().name(), "outcome", outcome.name(), "notes", request.notes()));
        return mapper.task(task);
    }

    private void applyOutcome(WorkflowTask task, TaskOutcome outcome, LocalDate effectiveDate) {
        PrisonerCase pc = task.getPrisonerCase();
        LocalDate date = effectiveDate == null ? LocalDate.now() : effectiveDate;
        LegalAssessment assessment = assessments.findFirstByPrisonerCaseIdAndCurrentTrueOrderByAssessedAtDesc(pc.getId()).orElse(null);
        if (task.getType() == TaskType.FILE_APPLICATION && assessment != null) {
            assessment.setStatus(EligibilityStatus.APPLICATION_FILED);
            assessments.save(assessment);
        }
        if (task.getType() == TaskType.RECORD_ORDER && outcome == TaskOutcome.BAIL_GRANTED) {
            pc.setBailGrantedDate(date);
            prisonerCases.save(pc);
            if (assessment != null) {
                assessment.setStatus(EligibilityStatus.BAIL_GRANTED);
                assessments.save(assessment);
            }
        }
        if (task.getType() == TaskType.EXECUTE_RELEASE) {
            pc.setPhysicalReleaseDate(date);
            pc.setActive(false);
            prisonerCases.save(pc);
            if (assessment != null) {
                assessment.setStatus(EligibilityStatus.RELEASED);
                assessments.save(assessment);
            }
        }
    }

    private void createNextTask(WorkflowTask completed, TaskOutcome outcome) {
        PrisonerCase pc = completed.getPrisonerCase();
        WorkflowTask next = switch (completed.getType()) {
            case VERIFY_RECORDS -> next(pc, TaskType.PREPARE_APPLICATION, UserRole.SUPERINTENDENT,
                    "Prepare statutory release application", 2);
            case PREPARE_APPLICATION -> next(pc, TaskType.FILE_APPLICATION, UserRole.SUPERINTENDENT,
                    "Digitally sign and file application with the court", 1);
            case FILE_APPLICATION -> next(pc, TaskType.SCHEDULE_HEARING, UserRole.COURT_REGISTRY,
                    "Acknowledge filing and schedule court consideration", 3);
            case SCHEDULE_HEARING -> next(pc, TaskType.RECORD_ORDER, UserRole.COURT_REGISTRY,
                    "Record the judicial outcome and written reasons", 7);
            case RECORD_ORDER -> outcome == TaskOutcome.BAIL_GRANTED
                    ? next(pc, TaskType.SATISFY_BOND, UserRole.DLSA_COUNSEL,
                        "Complete personal bond or bail-bond requirements", 1)
                    : outcome == TaskOutcome.CONTINUED_DETENTION
                        ? next(pc, TaskType.VERIFY_RECORDS, UserRole.DLSA_COUNSEL,
                            "Review recorded reasons and next legal remedy", 30) : null;
            case SATISFY_BOND -> next(pc, TaskType.EXECUTE_RELEASE, UserRole.SUPERINTENDENT,
                    "Verify all holds and execute physical release", 1);
            default -> null;
        };
        if (next != null) tasks.save(next);
    }

    private WorkflowTask next(PrisonerCase pc, TaskType type, UserRole role, String title, int dueInDays) {
        return WorkflowTask.builder().prisonerCase(pc).type(type).assignedRole(role).title(title)
                .dueAt(Instant.now().plus(dueInDays, ChronoUnit.DAYS)).build();
    }

    private WorkflowTask load(UUID id) {
        return tasks.findById(id).orElseThrow(() -> new ResourceNotFoundException("Workflow task not found"));
    }
}

