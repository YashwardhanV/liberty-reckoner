package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.NotificationView;
import in.gov.libertyreckoner.domain.NotificationStatus;
import in.gov.libertyreckoner.service.NotificationOutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN','AUDITOR')")
public class NotificationController {
    private final NotificationOutboxService outbox;

    @GetMapping("/outbox")
    public List<NotificationView> recent(@RequestParam(required = false) NotificationStatus status) {
        return outbox.recent(status).stream().map(notification -> new NotificationView(
                notification.getId(), notification.getPrisonerCase().getId(),
                notification.getPrisonerCase().getPrisoner().getPrisonNumber(), notification.getChannel(),
                notification.getRecipientRole(), notification.getRecipientAddress(), notification.getSubject(),
                notification.getStatus(), notification.getAttempts(), notification.getNextAttemptAt(),
                notification.getLastError(), notification.getCreatedAt(), notification.getDeliveredAt())).toList();
    }
}
