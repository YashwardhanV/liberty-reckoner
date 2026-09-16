package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class PrisonerService {
    private final PrisonerRepository prisoners;
    private final LegalAssessmentRepository assessments;
    private final ApiMapper mapper;

    @Transactional(readOnly = true)
    public PageResponse<PrisonerListView> list(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, Math.min(Math.max(size, 1), 100), Sort.by("fullName").ascending());
        Page<Prisoner> result = query == null || query.isBlank()
                ? prisoners.findAll(pageable)
                : prisoners.findByFullNameContainingIgnoreCaseOrPrisonNumberContainingIgnoreCase(query, query, pageable);
        List<PrisonerListView> content = result.getContent().stream().map(this::listView).toList();
        return new PageResponse<>(content, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public PrisonerView detail(UUID id) {
        Prisoner prisoner = prisoners.findDetailedById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Prisoner not found"));
        Prison prison = prisoner.getPrison();
        List<PrisonerCaseView> caseViews = prisoner.getPrisonerCases().stream()
                .sorted(Comparator.comparing((PrisonerCase pc) -> pc.getLegalCase().getNextHearingDate(),
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(pc -> mapper.prisonerCase(pc, current(pc))).toList();
        return new PrisonerView(prisoner.getId(), prisoner.getPrisonNumber(), prisoner.getFullName(),
                prisoner.getGender(), prisoner.getDateOfBirth(), prisoner.getNationality(),
                prisoner.getPreferredLanguage(), prisoner.getPreviousConvictions(),
                prisoner.isConvictionHistoryVerified(), new PrisonView(prison.getId(), prison.getCode(),
                prison.getName(), prison.getDistrict(), prison.getState(), prison.getCapacity(),
                prison.getCurrentPopulation()), caseViews);
    }

    private PrisonerListView listView(Prisoner prisoner) {
        List<PrisonerCase> activeCases = prisoner.getPrisonerCases().stream().filter(PrisonerCase::isActive).toList();
        LegalAssessment mostUrgent = activeCases.stream().map(this::current).filter(java.util.Objects::nonNull)
                .min(Comparator.comparing(a -> a.getDaysRemaining() == null ? Integer.MAX_VALUE : a.getDaysRemaining()))
                .orElse(null);
        LocalDate nextHearing = activeCases.stream().map(pc -> pc.getLegalCase().getNextHearingDate())
                .filter(java.util.Objects::nonNull).min(LocalDate::compareTo).orElse(null);
        return new PrisonerListView(prisoner.getId(), prisoner.getPrisonNumber(), prisoner.getFullName(),
                prisoner.getGender(), prisoner.getPrison().getName(), prisoner.getPrison().getDistrict(),
                activeCases.size(), mostUrgent == null ? EligibilityStatus.DATA_INCOMPLETE : mostUrgent.getStatus(),
                mostUrgent == null ? null : mostUrgent.getDaysRemaining(),
                mostUrgent == null ? 0 : mostUrgent.getCreditedCustodyDays(), nextHearing);
    }

    private LegalAssessment current(PrisonerCase pc) {
        return assessments.findFirstByPrisonerCaseIdAndCurrentTrueOrderByAssessedAtDesc(pc.getId()).orElse(null);
    }
}
