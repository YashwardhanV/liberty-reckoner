package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "legal_assessment")
public class LegalAssessment {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prisoner_case_id", nullable = false)
    private PrisonerCase prisonerCase;
    @Column(nullable = false)
    @Builder.Default
    private Instant assessedAt = Instant.now();
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private EligibilityStatus status;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private VerificationStatus verificationStatus;
    private Integer maximumTermDays;
    private Integer thresholdDays;
    @Column(nullable = false)
    private int creditedCustodyDays;
    @Column(nullable = false)
    private int excludedDelayDays;
    private LocalDate projectedThresholdDate;
    private Integer daysRemaining;
    @Column(nullable = false, length = 30)
    private String thresholdFraction;
    @Column(nullable = false, length = 40)
    private String ruleVersion;
    @Column(nullable = false, columnDefinition = "text")
    private String explanation;
    @Column(nullable = false, columnDefinition = "text")
    private String blockersJson;
    @Column(name = "is_current", nullable = false)
    @Builder.Default
    private boolean current = true;
}
