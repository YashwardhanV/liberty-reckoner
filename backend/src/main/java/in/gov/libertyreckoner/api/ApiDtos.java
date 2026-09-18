package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.domain.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class ApiDtos {
    private ApiDtos() { }

    public record LoginRequest(@Email @NotBlank String email, @NotBlank String password) { }
    public record UserView(UUID id, String fullName, String email, UserRole role) { }
    public record LoginResponse(String accessToken, long expiresInSeconds, UserView user) { }

    public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) { }

    public record AssessmentView(
            UUID id,
            EligibilityStatus status,
            VerificationStatus verificationStatus,
            Integer maximumTermDays,
            Integer thresholdDays,
            int creditedCustodyDays,
            int excludedDelayDays,
            LocalDate projectedThresholdDate,
            Integer daysRemaining,
            String thresholdFraction,
            String ruleVersion,
            String explanation,
            List<String> blockers,
            Instant assessedAt) { }

    public record PrisonerListView(
            UUID id,
            String prisonNumber,
            String fullName,
            Gender gender,
            String prisonName,
            String district,
            int activeCases,
            EligibilityStatus status,
            Integer daysRemaining,
            int custodyDays,
            LocalDate nextHearingDate) { }

    public record PrisonerView(
            UUID id,
            String prisonNumber,
            String fullName,
            Gender gender,
            LocalDate dateOfBirth,
            String nationality,
            String preferredLanguage,
            @PositiveOrZero int previousConvictions,
            boolean convictionHistoryVerified,
            PrisonView prison,
            List<PrisonerCaseView> cases) { }

    public record PrisonView(UUID id, String code, String name, String district, String state,
                             int capacity, int currentPopulation) { }

    public record PrisonerCaseView(
            UUID id,
            String cnrNumber,
            String firNumber,
            String policeStation,
            String courtName,
            CaseStage stage,
            LocalDate chargeSheetDate,
            LocalDate nextHearingDate,
            boolean active,
            @PositiveOrZero int accusedDelayDays,
            boolean accusedDelayVerified,
            LocalDate bailGrantedDate,
            LocalDate physicalReleaseDate,
            List<ChargeView> charges,
            List<CustodyView> custodyPeriods,
            AssessmentView assessment,
            List<TaskView> tasks) { }

    public record ChargeView(UUID id, String actName, String sectionCode, String description,
                             Integer maximumTermDays, String maximumTermLabel,
                             boolean deathPunishmentPossible, boolean lifeImprisonmentPossible,
                             LocalDate effectiveFrom, String legalSource) { }

    public record CustodyView(UUID id, LocalDate startDate, LocalDate endDate, boolean included,
                              String exclusionReason, String sourceSystem, boolean verified) { }

    public record TaskView(UUID id, UUID prisonerId, UUID prisonerCaseId, String prisonerName, String prisonNumber,
                           String cnrNumber, TaskType type, UserRole assignedRole, TaskStatus status,
                           String title, String notes, Instant dueAt, Instant completedAt, Instant createdAt) { }

    public record CompleteTaskRequest(@NotBlank String notes, TaskOutcome outcome, LocalDate effectiveDate) { }

    public record StatusCount(EligibilityStatus status, long count) { }
    public record OccupancyView(String prisonName, String district, int capacity, int population, int percentage) { }
    public record UrgentCaseView(UUID prisonerId, UUID prisonerCaseId, String prisonerName,
                                 String prisonNumber, String cnrNumber, String prisonName,
                                 EligibilityStatus status, Integer daysRemaining, int custodyDays,
                                 LocalDate nextHearingDate) { }

    public record DashboardView(
            long totalUndertrials,
            long actionDue,
            long dueSoon,
            long legalReview,
            long openTasks,
            long overdueTasks,
            int overallOccupancyPercentage,
            List<StatusCount> statusDistribution,
            List<OccupancyView> occupancy,
            List<UrgentCaseView> urgentCases,
            Instant generatedAt) { }

    public record ApplicationPacketView(
            UUID prisonerCaseId,
            String documentNumber,
            String heading,
            String recipient,
            String subject,
            List<String> verifiedFacts,
            String body,
            String verificationStatement,
            String generatedBy,
            Instant generatedAt) { }

    public record RuleView(String id, String title, String threshold, String applicability,
                           String systemTreatment, String authority) { }

    public record JusticeRecordUpsertRequest(
            @NotBlank String sourceSystem,
            @NotBlank String sourceEventId,
            @NotBlank String prisonCode,
            @NotBlank String prisonNumber,
            @NotBlank String fullName,
            @NotNull Gender gender,
            LocalDate dateOfBirth,
            @NotBlank String nationality,
            @NotBlank String preferredLanguage,
            int previousConvictions,
            boolean convictionHistoryVerified,
            @NotBlank String cnrNumber,
            @NotBlank String firNumber,
            @NotBlank String policeStation,
            @NotBlank String courtName,
            @NotBlank String district,
            @NotBlank String state,
            @NotNull CaseStage stage,
            LocalDate chargeSheetDate,
            LocalDate nextHearingDate,
            int accusedDelayDays,
            boolean accusedDelayVerified,
            @NotNull List<@Valid ChargeInput> charges,
            @NotNull List<@Valid CustodyInput> custodyPeriods) { }

    public record ChargeInput(
            @NotBlank String actName,
            @NotBlank String sectionCode,
            @NotBlank String description,
            @Positive Integer maximumTermDays,
            String maximumTermLabel,
            boolean deathPunishmentPossible,
            boolean lifeImprisonmentPossible,
            @NotNull LocalDate effectiveFrom,
            @NotBlank String legalSource) { }

    public record CustodyInput(
            @NotNull LocalDate startDate,
            LocalDate endDate,
            boolean included,
            String exclusionReason,
            @NotBlank String sourceSystem,
            boolean verified) { }

    public record IntegrationResult(String outcome, UUID prisonerId, UUID prisonerCaseId,
                                    String sourceEventId, AssessmentView assessment) { }

    public record NotificationView(UUID id, UUID prisonerCaseId, String prisonerNumber,
                                   NotificationChannel channel, UserRole recipientRole,
                                   String recipientAddress, String subject, NotificationStatus status,
                                   int attempts, Instant nextAttemptAt, String lastError,
                                   Instant createdAt, Instant deliveredAt) { }

    public record ErrorView(Instant timestamp, int status, String code, String message,
                            String path, String requestId, java.util.Map<String, String> fieldErrors) { }
}
