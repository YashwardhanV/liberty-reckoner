package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.TaskStatus;
import in.gov.libertyreckoner.domain.WorkflowTask;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface WorkflowTaskRepository extends JpaRepository<WorkflowTask, UUID> {
    List<WorkflowTask> findByPrisonerCaseIdOrderByCreatedAtAsc(UUID prisonerCaseId);
    List<WorkflowTask> findByStatusInOrderByDueAtAsc(List<TaskStatus> statuses);
    long countByStatusInAndDueAtBefore(List<TaskStatus> statuses, Instant dueAt);
    boolean existsByPrisonerCaseIdAndStatusIn(UUID prisonerCaseId, List<TaskStatus> statuses);
}
