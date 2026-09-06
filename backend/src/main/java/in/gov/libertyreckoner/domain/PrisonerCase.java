package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "prisoner_case", uniqueConstraints = @UniqueConstraint(columnNames = {"prisoner_id", "legal_case_id"}))
public class PrisonerCase {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prisoner_id", nullable = false)
    private Prisoner prisoner;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "legal_case_id", nullable = false)
    private LegalCase legalCase;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    @Column(nullable = false)
    @Builder.Default
    private int accusedDelayDays = 0;
    @Column(nullable = false)
    @Builder.Default
    private boolean accusedDelayVerified = true;
    private LocalDate bailGrantedDate;
    private LocalDate physicalReleaseDate;
    @OneToMany(mappedBy = "prisonerCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Charge> charges = new ArrayList<>();
    @OneToMany(mappedBy = "prisonerCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CustodyPeriod> custodyPeriods = new ArrayList<>();
    @OneToMany(mappedBy = "prisonerCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<LegalAssessment> assessments = new ArrayList<>();
    @OneToMany(mappedBy = "prisonerCase", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<WorkflowTask> workflowTasks = new ArrayList<>();
    @Version
    private long version;
}
