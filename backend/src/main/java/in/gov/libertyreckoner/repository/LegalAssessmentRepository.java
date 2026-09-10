package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.EligibilityStatus;
import in.gov.libertyreckoner.domain.LegalAssessment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LegalAssessmentRepository extends JpaRepository<LegalAssessment, UUID> {
    Optional<LegalAssessment> findFirstByPrisonerCaseIdAndCurrentTrueOrderByAssessedAtDesc(UUID prisonerCaseId);
    List<LegalAssessment> findByCurrentTrueOrderByDaysRemainingAsc();
    long countByCurrentTrueAndStatus(EligibilityStatus status);

    @Modifying
    @Query("update LegalAssessment a set a.current = false where a.prisonerCase.id = :caseId and a.current = true")
    void markExistingAssessmentsHistorical(@Param("caseId") UUID caseId);
}
