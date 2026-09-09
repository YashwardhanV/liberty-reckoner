package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.Charge;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface ChargeRepository extends JpaRepository<Charge, UUID> { }

