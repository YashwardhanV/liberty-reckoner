package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "legal_case")
public class LegalCase {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 40)
    private String cnrNumber;
    @Column(nullable = false, length = 60)
    private String firNumber;
    @Column(nullable = false, length = 160)
    private String policeStation;
    @Column(nullable = false, length = 200)
    private String courtName;
    @Column(nullable = false, length = 120)
    private String district;
    @Column(nullable = false, length = 120)
    private String state;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private CaseStage stage;
    private LocalDate chargeSheetDate;
    private LocalDate nextHearingDate;
    @OneToMany(mappedBy = "legalCase")
    @Builder.Default
    private List<PrisonerCase> prisonerCases = new ArrayList<>();
}

