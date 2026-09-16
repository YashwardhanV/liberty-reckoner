package in.gov.libertyreckoner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Map;

@Service @RequiredArgsConstructor
public class JusticeIntegrationService {
    private final IntegrationEventRepository integrationEvents;
    private final PrisonRepository prisons;
    private final PrisonerRepository prisoners;
    private final LegalCaseRepository legalCases;
    private final PrisonerCaseRepository prisonerCases;
    private final LegalAssessmentRepository assessments;
    private final EligibilityService eligibilityService;
    private final ApiMapper mapper;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public IntegrationResult upsert(JusticeRecordUpsertRequest request) {
        String hash = hash(request);
        IntegrationEvent prior = integrationEvents
                .findBySourceSystemIgnoreCaseAndSourceEventId(request.sourceSystem(), request.sourceEventId())
                .orElse(null);
        if (prior != null) {
            if (!prior.getPayloadHash().equals(hash)) {
                throw new BusinessRuleException("The source event ID was already used with a different payload");
            }
            PrisonerCase pc = prisonerCases.findDetailedById(prior.getPrisonerCaseId())
                    .orElseThrow(() -> new ResourceNotFoundException("Previously integrated case not found"));
            LegalAssessment current = assessments.findFirstByPrisonerCaseIdAndCurrentTrueOrderByAssessedAtDesc(pc.getId()).orElse(null);
            return new IntegrationResult("ALREADY_PROCESSED", pc.getPrisoner().getId(), pc.getId(),
                    request.sourceEventId(), mapper.assessment(current));
        }

        Prison prison = prisons.findByCodeIgnoreCase(request.prisonCode())
                .orElseThrow(() -> new ResourceNotFoundException("Unknown prison code: " + request.prisonCode()));
        Prisoner prisoner = prisoners.findByPrisonNumberIgnoreCase(request.prisonNumber()).orElseGet(Prisoner::new);
        if (prisoner.getId() == null) prisoner.setId(java.util.UUID.randomUUID());
        prisoner.setPrisonNumber(request.prisonNumber().trim().toUpperCase());
        prisoner.setFullName(request.fullName().trim());
        prisoner.setGender(request.gender());
        prisoner.setDateOfBirth(request.dateOfBirth());
        prisoner.setNationality(request.nationality().trim());
        prisoner.setPreferredLanguage(request.preferredLanguage().trim());
        prisoner.setPreviousConvictions(request.previousConvictions());
        prisoner.setConvictionHistoryVerified(request.convictionHistoryVerified());
        prisoner.setPrison(prison);
        prisoner = prisoners.save(prisoner);

        LegalCase legalCase = legalCases.findByCnrNumberIgnoreCase(request.cnrNumber()).orElseGet(LegalCase::new);
        if (legalCase.getId() == null) legalCase.setId(java.util.UUID.randomUUID());
        legalCase.setCnrNumber(request.cnrNumber().trim().toUpperCase());
        legalCase.setFirNumber(request.firNumber().trim());
        legalCase.setPoliceStation(request.policeStation().trim());
        legalCase.setCourtName(request.courtName().trim());
        legalCase.setDistrict(request.district().trim());
        legalCase.setState(request.state().trim());
        legalCase.setStage(request.stage());
        legalCase.setChargeSheetDate(request.chargeSheetDate());
        legalCase.setNextHearingDate(request.nextHearingDate());
        legalCase = legalCases.save(legalCase);

        PrisonerCase pc = prisonerCases.findByPrisonerIdAndLegalCaseId(prisoner.getId(), legalCase.getId())
                .orElseGet(PrisonerCase::new);
        boolean created = pc.getId() == null;
        if (pc.getId() == null) pc.setId(java.util.UUID.randomUUID());
        pc.setPrisoner(prisoner);
        pc.setLegalCase(legalCase);
        pc.setActive(request.stage() != CaseStage.DISPOSED);
        pc.setAccusedDelayDays(request.accusedDelayDays());
        pc.setAccusedDelayVerified(request.accusedDelayVerified());
        PrisonerCase targetCase = pc;
        pc.getCharges().clear();
        request.charges().forEach(input -> targetCase.getCharges().add(Charge.builder().prisonerCase(targetCase)
                .actName(input.actName()).sectionCode(input.sectionCode()).description(input.description())
                .maximumTermDays(input.maximumTermDays()).maximumTermLabel(input.maximumTermLabel())
                .deathPunishmentPossible(input.deathPunishmentPossible())
                .lifeImprisonmentPossible(input.lifeImprisonmentPossible()).effectiveFrom(input.effectiveFrom())
                .legalSource(input.legalSource()).build()));
        pc.getCustodyPeriods().clear();
        request.custodyPeriods().forEach(input -> targetCase.getCustodyPeriods().add(CustodyPeriod.builder()
                .prisonerCase(targetCase).startDate(input.startDate()).endDate(input.endDate()).included(input.included())
                .exclusionReason(input.exclusionReason()).sourceSystem(input.sourceSystem()).verified(input.verified()).build()));
        pc = prisonerCases.saveAndFlush(pc);

        AssessmentView assessment = eligibilityService.evaluate(pc.getId());
        integrationEvents.save(IntegrationEvent.builder().sourceSystem(request.sourceSystem().trim())
                .sourceEventId(request.sourceEventId().trim()).payloadHash(hash).status(IntegrationStatus.PROCESSED)
                .processedAt(Instant.now()).prisonerCaseId(pc.getId()).build());
        auditService.record("PRISONER_CASE", pc.getId(), "JUSTICE_RECORD_SYNCHRONIZED",
                Map.of("sourceSystem", request.sourceSystem(), "sourceEventId", request.sourceEventId()));
        return new IntegrationResult(created ? "CREATED" : "UPDATED", prisoner.getId(), pc.getId(),
                request.sourceEventId(), assessment);
    }

    private String hash(JusticeRecordUpsertRequest request) {
        try {
            byte[] payload = objectMapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(payload));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash integration payload", exception);
        }
    }
}
