package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "integration_event", uniqueConstraints = @UniqueConstraint(columnNames = {"source_system", "source_event_id"}))
public class IntegrationEvent {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @Column(nullable = false, length = 80)
    private String sourceSystem;
    @Column(nullable = false, length = 120)
    private String sourceEventId;
    @Column(nullable = false, length = 64)
    private String payloadHash;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    private IntegrationStatus status;
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant receivedAt = Instant.now();
    @Column(nullable = false)
    @Builder.Default
    private Instant processedAt = Instant.now();
    @Column(nullable = false)
    private UUID prisonerCaseId;
}

