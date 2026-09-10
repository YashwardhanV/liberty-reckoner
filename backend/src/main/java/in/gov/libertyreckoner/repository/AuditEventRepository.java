package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.AuditEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {
    List<AuditEvent> findTop50ByAggregateTypeAndAggregateIdOrderByOccurredAtDesc(String aggregateType, UUID aggregateId);
}

