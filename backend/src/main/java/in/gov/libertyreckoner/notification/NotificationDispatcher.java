package in.gov.libertyreckoner.notification;

import in.gov.libertyreckoner.service.NotificationOutboxService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(prefix = "libertyreckoner.notifications", name = "dispatch-enabled",
        havingValue = "true", matchIfMissing = true)
public class NotificationDispatcher {
    private final NotificationOutboxService outbox;
    private final NotificationDeliveryPort deliveryPort;
    private final MeterRegistry meters;

    @Scheduled(fixedDelayString = "${libertyreckoner.notifications.poll-interval-ms:10000}")
    public void dispatch() {
        for (UUID id : outbox.claimBatch()) {
            try {
                deliveryPort.send(outbox.message(id));
                outbox.markDelivered(id);
                meters.counter("libertyreckoner.notifications", "outcome", "delivered").increment();
            } catch (Exception exception) {
                log.warn("Notification delivery failed [id={}]", id, exception);
                outbox.markFailed(id, exception);
                meters.counter("libertyreckoner.notifications", "outcome", "failed").increment();
            }
        }
    }
}
