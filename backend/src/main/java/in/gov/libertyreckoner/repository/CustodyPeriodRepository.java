package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.CustodyPeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CustodyPeriodRepository extends JpaRepository<CustodyPeriod, UUID> { }

