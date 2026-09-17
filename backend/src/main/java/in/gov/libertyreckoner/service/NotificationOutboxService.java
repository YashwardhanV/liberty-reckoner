package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.notification.NotificationDeliveryPort.NotificationMessage;
import in.gov.libertyreckoner.repository.OutboundNotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@RequiredArgsConstructor
public class NotificationOutboxService {
    private final OutboundNotificationRepository notifications;

    @Value("${libertyreckoner.notifications.batch-size:25}")
    private int batchSize;
    @Value("${libertyreckoner.notifications.max-attempts:5}")
    private int maxAttempts;
    @Value("${libertyreckoner.notifications.from-address}")
    private String fromAddress;
    @Value("${libertyreckoner.notifications.superintendent-address}")
    private String superintendentAddress;
    @Value("${libertyreckoner.notifications.dlsa-address}")
    private String dlsaAddress;
    @Value("${libertyreckoner.notifications.court-address}")
    private String courtAddress;

    @Transactional
    public void enqueueEligibilityAlerts(LegalAssessment assessment) {
        PrisonerCase prisonerCase = assessment.getPrisonerCase();
        Prisoner prisoner = prisonerCase.getPrisoner();
        String subject = "Statutory custody alert: " + prisoner.getPrisonNumber();
        String body = """
                Liberty Reckoner has recorded a statutory custody action signal.

                Prisoner: %s (%s)
                CNR: %s
                Status: %s
                Credited custody: %d days
                Threshold: %s days
                Rule version: %s

                This is decision support, not a judicial order. Verify the source record and complete the accountable workflow.
                """.formatted(prisoner.getFullName(), prisoner.getPrisonNumber(),
                prisonerCase.getLegalCase().getCnrNumber(), assessment.getStatus(),
                assessment.getCreditedCustodyDays(), Objects.toString(assessment.getThresholdDays(), "review required"),
                assessment.getRuleVersion());

        List<Recipient> recipients = List.of(
                new Recipient(UserRole.SUPERINTENDENT, superintendentAddress),
                new Recipient(UserRole.DLSA_COUNSEL, dlsaAddress),
                new Recipient(UserRole.COURT_REGISTRY, courtAddress));
        recipients.forEach(recipient -> enqueue(assessment, recipient, subject, body));
    }

    @Transactional
    public List<UUID> claimBatch() {
        Instant now = Instant.now();
        notifications.recoverStaleClaims(NotificationStatus.PROCESSING, NotificationStatus.FAILED, now,
                now.minus(Duration.ofMinutes(5)), "Recovered after an interrupted delivery attempt");
        List<OutboundNotification> claimed = notifications.findClaimable(
                List.of(NotificationStatus.PENDING, NotificationStatus.FAILED), now,
                PageRequest.of(0, Math.min(Math.max(batchSize, 1), 100)));
        claimed.forEach(notification -> {
            notification.setStatus(NotificationStatus.PROCESSING);
            notification.setLockedAt(now);
            notification.setAttempts(notification.getAttempts() + 1);
        });
        return notifications.saveAll(claimed).stream().map(OutboundNotification::getId).toList();
    }

    @Transactional(readOnly = true)
    public NotificationMessage message(UUID id) {
        OutboundNotification notification = load(id);
        if (notification.getStatus() != NotificationStatus.PROCESSING) {
            throw new BusinessRuleException("Notification is not claimed for delivery");
        }
        return new NotificationMessage(notification.getId(), fromAddress, notification.getRecipientAddress(),
                notification.getSubject(), notification.getBody());
    }

    @Transactional
    public void markDelivered(UUID id) {
        OutboundNotification notification = load(id);
        notification.setStatus(NotificationStatus.DELIVERED);
        notification.setDeliveredAt(Instant.now());
        notification.setLockedAt(null);
        notification.setLastError(null);
    }

    @Transactional
    public void markFailed(UUID id, Exception exception) {
        OutboundNotification notification = load(id);
        notification.setLockedAt(null);
        notification.setLastError(truncate(exception.getMessage()));
        if (notification.getAttempts() >= maxAttempts) {
            notification.setStatus(NotificationStatus.DEAD_LETTER);
            return;
        }
        notification.setStatus(NotificationStatus.FAILED);
        long retrySeconds = Math.min(3600L, 30L << Math.min(notification.getAttempts() - 1, 6));
        notification.setNextAttemptAt(Instant.now().plusSeconds(retrySeconds));
    }

    @Transactional(readOnly = true)
    public List<OutboundNotification> recent(NotificationStatus status) {
        return status == null ? notifications.findTop100ByOrderByCreatedAtDesc()
                : notifications.findTop100ByStatusOrderByCreatedAtDesc(status);
    }

    private void enqueue(LegalAssessment assessment, Recipient recipient, String subject, String body) {
        String eventKey = "eligibility:" + assessment.getId() + ":" + recipient.role();
        if (notifications.existsByEventKey(eventKey)) return;
        notifications.save(OutboundNotification.builder().eventKey(eventKey)
                .prisonerCase(assessment.getPrisonerCase()).recipientRole(recipient.role())
                .recipientAddress(recipient.address()).subject(subject).body(body).build());
    }

    private OutboundNotification load(UUID id) {
        return notifications.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
    }

    private String truncate(String message) {
        String safe = message == null || message.isBlank() ? "Delivery provider failed" : message;
        return safe.substring(0, Math.min(safe.length(), 500));
    }

    private record Recipient(UserRole role, String address) { }
}
