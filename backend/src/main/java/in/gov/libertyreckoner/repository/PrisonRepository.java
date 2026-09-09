package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.Prison;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PrisonRepository extends JpaRepository<Prison, UUID> {
    Optional<Prison> findByCodeIgnoreCase(String code);
}
