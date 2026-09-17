package in.gov.libertyreckoner.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "libertyreckoner.notifications", name = "transport",
        havingValue = "log", matchIfMissing = true)
public class LoggingNotificationAdapter implements NotificationDeliveryPort {
    @Override
    public void send(NotificationMessage message) {
        log.info("Demonstration notification delivered [id={}, to={}, subject={}]",
                message.id(), message.to(), message.subject());
    }
}
