package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.IntegrationEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface IntegrationEventRepository extends JpaRepository<IntegrationEvent, UUID> {
    Optional<IntegrationEvent> findBySourceSystemIgnoreCaseAndSourceEventId(String sourceSystem, String sourceEventId);
}
