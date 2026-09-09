package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity
@Table(name = "outbound_notification", uniqueConstraints = @UniqueConstraint(columnNames = "event_key"))
public class OutboundNotification {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @Column(name = "event_key", nullable = false, length = 180)
    private String eventKey;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prisoner_case_id", nullable = false)
    private PrisonerCase prisonerCase;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    @Builder.Default
    private NotificationChannel channel = NotificationChannel.EMAIL;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 40)
    private UserRole recipientRole;
    @Column(nullable = false, length = 240)
    private String recipientAddress;
    @Column(nullable = false, length = 240)
    private String subject;
    @Column(nullable = false, columnDefinition = "text")
    private String body;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30)
    @Builder.Default
    private NotificationStatus status = NotificationStatus.PENDING;
    @Column(nullable = false)
    @Builder.Default
    private int attempts = 0;
    @Column(nullable = false)
    @Builder.Default
    private Instant nextAttemptAt = Instant.now();
    private Instant lockedAt;
    @Column(length = 500)
    private String lastError;
    @Column(nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
    private Instant deliveredAt;
    @Version
    private long version;
}
