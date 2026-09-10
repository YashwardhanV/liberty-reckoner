package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.PrisonerCase;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrisonerCaseRepository extends JpaRepository<PrisonerCase, UUID> {
    Optional<PrisonerCase> findDetailedById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select prisonerCase from PrisonerCase prisonerCase where prisonerCase.id = :id")
    Optional<PrisonerCase> findForEvaluationById(@Param("id") UUID id);

    long countByPrisonerIdAndActiveTrue(UUID prisonerId);

    List<PrisonerCase> findByActiveTrue();
    @Query("select prisonerCase.id from PrisonerCase prisonerCase where prisonerCase.active = true")
    List<UUID> findActiveIds();
    Optional<PrisonerCase> findByPrisonerIdAndLegalCaseId(UUID prisonerId, UUID legalCaseId);
}
