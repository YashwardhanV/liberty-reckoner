package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "custody_period")
public class CustodyPeriod {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prisoner_case_id", nullable = false)
    private PrisonerCase prisonerCase;
    @Column(nullable = false)
    private LocalDate startDate;
    private LocalDate endDate;
    @Column(nullable = false)
    @Builder.Default
    private boolean included = true;
    @Column(length = 240)
    private String exclusionReason;
    @Column(nullable = false, length = 60)
    private String sourceSystem;
    @Column(nullable = false)
    @Builder.Default
    private boolean verified = true;
}

