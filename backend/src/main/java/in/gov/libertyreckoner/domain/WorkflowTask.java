package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "workflow_task")
public class WorkflowTask {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prisoner_case_id", nullable = false)
    private PrisonerCase prisonerCase;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 50)
    private TaskType type;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private UserRole assignedRole;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    @Builder.Default
    private TaskStatus status = TaskStatus.OPEN;
    @Column(nullable = false, length = 220)
    private String title;
    @Column(columnDefinition = "text")
    private String notes;
    @Column(nullable = false)
    private Instant dueAt;
    private Instant completedAt;
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    @Version
    private long version;
}
