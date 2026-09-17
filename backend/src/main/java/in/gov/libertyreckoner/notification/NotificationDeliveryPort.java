package in.gov.libertyreckoner.notification;

import java.util.UUID;

public interface NotificationDeliveryPort {
    void send(NotificationMessage message);

    record NotificationMessage(UUID id, String from, String to, String subject, String body) { }
}
