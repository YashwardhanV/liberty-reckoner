package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "charge")
public class Charge {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prisoner_case_id", nullable = false)
    private PrisonerCase prisonerCase;
    @Column(nullable = false, length = 180)
    private String actName;
    @Column(nullable = false, length = 40)
    private String sectionCode;
    @Column(nullable = false, length = 240)
    private String description;
    private Integer maximumTermDays;
    @Column(length = 80)
    private String maximumTermLabel;
    @Column(nullable = false)
    private boolean deathPunishmentPossible;
    @Column(nullable = false)
    private boolean lifeImprisonmentPossible;
    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
    @Column(nullable = false)
    private LocalDate effectiveFrom;
    @Column(nullable = false, length = 80)
    private String legalSource;
}

