package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "prisoner")
public class Prisoner {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 40)
    private String prisonNumber;
    @Column(nullable = false, length = 160)
    private String fullName;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private Gender gender;
    private LocalDate dateOfBirth;
    @Column(nullable = false, length = 80)
    private String nationality;
    @Column(nullable = false, length = 80)
    private String preferredLanguage;
    @Column(nullable = false)
    private int previousConvictions;
    @Column(nullable = false)
    private boolean convictionHistoryVerified;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prison_id", nullable = false)
    private Prison prison;
    @OneToMany(mappedBy = "prisoner", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PrisonerCase> prisonerCases = new ArrayList<>();
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}

