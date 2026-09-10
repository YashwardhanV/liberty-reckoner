package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.Prisoner;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface PrisonerRepository extends JpaRepository<Prisoner, UUID> {
    Optional<Prisoner> findByPrisonNumberIgnoreCase(String prisonNumber);

    Page<Prisoner> findByFullNameContainingIgnoreCaseOrPrisonNumberContainingIgnoreCase(
            String fullName, String prisonNumber, Pageable pageable);

    Optional<Prisoner> findDetailedById(UUID id);
}
