package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.LegalCase;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface LegalCaseRepository extends JpaRepository<LegalCase, UUID> {
    Optional<LegalCase> findByCnrNumberIgnoreCase(String cnrNumber);
}
