package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "audit_event")
public class AuditEvent {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @Column(nullable = false, length = 60)
    private String aggregateType;
    @Column(nullable = false)
    private UUID aggregateId;
    @Column(nullable = false, length = 80)
    private String action;
    @Column(nullable = false, length = 180)
    private String actor;
    @Column(nullable = false, columnDefinition = "text")
    private String detailsJson;
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant occurredAt = Instant.now();
}
