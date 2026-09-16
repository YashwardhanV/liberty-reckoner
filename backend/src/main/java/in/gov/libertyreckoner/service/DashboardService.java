package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.*;

@Service @RequiredArgsConstructor
public class DashboardService {
    private final PrisonerRepository prisoners;
    private final PrisonRepository prisons;
    private final LegalAssessmentRepository assessments;
    private final WorkflowTaskRepository tasks;

    @Transactional(readOnly = true)
    public DashboardView summary() {
        List<LegalAssessment> current = assessments.findByCurrentTrueOrderByDaysRemainingAsc();
        Map<EligibilityStatus, Long> counts = new EnumMap<>(EligibilityStatus.class);
        current.forEach(a -> counts.merge(a.getStatus(), 1L, Long::sum));
        List<StatusCount> distribution = Arrays.stream(EligibilityStatus.values())
                .filter(status -> counts.getOrDefault(status, 0L) > 0)
                .map(status -> new StatusCount(status, counts.get(status))).toList();

        List<Prison> prisonEntities = prisons.findAll();
        int totalCapacity = prisonEntities.stream().mapToInt(Prison::getCapacity).sum();
        int totalPopulation = prisonEntities.stream().mapToInt(Prison::getCurrentPopulation).sum();
        List<OccupancyView> occupancy = prisonEntities.stream().map(p -> new OccupancyView(
                p.getName(), p.getDistrict(), p.getCapacity(), p.getCurrentPopulation(),
                percentage(p.getCurrentPopulation(), p.getCapacity()))).toList();

        Set<EligibilityStatus> urgentStatuses = EnumSet.of(EligibilityStatus.ACTION_OVERDUE,
                EligibilityStatus.MAXIMUM_REACHED, EligibilityStatus.DUE_SOON,
                EligibilityStatus.DATA_INCOMPLETE, EligibilityStatus.BAIL_GRANTED);
        List<UrgentCaseView> urgent = current.stream().filter(a -> urgentStatuses.contains(a.getStatus()))
                .sorted(Comparator.comparingInt(this::urgencyScore)
                        .thenComparing(a -> a.getDaysRemaining() == null ? Integer.MAX_VALUE : a.getDaysRemaining()))
                .limit(8).map(this::urgentView).toList();

        List<TaskStatus> openStatuses = List.of(TaskStatus.OPEN, TaskStatus.IN_PROGRESS);
        long openTasks = tasks.findByStatusInOrderByDueAtAsc(openStatuses).size();
        long overdueTasks = tasks.countByStatusInAndDueAtBefore(openStatuses, Instant.now());
        return new DashboardView(prisoners.count(),
                count(counts, EligibilityStatus.ACTION_OVERDUE) + count(counts, EligibilityStatus.MAXIMUM_REACHED),
                count(counts, EligibilityStatus.DUE_SOON), count(counts, EligibilityStatus.LEGAL_REVIEW),
                openTasks, overdueTasks, percentage(totalPopulation, totalCapacity), distribution,
                occupancy, urgent, Instant.now());
    }

    private UrgentCaseView urgentView(LegalAssessment assessment) {
        PrisonerCase pc = assessment.getPrisonerCase();
        Prisoner prisoner = pc.getPrisoner();
        return new UrgentCaseView(prisoner.getId(), pc.getId(), prisoner.getFullName(),
                prisoner.getPrisonNumber(), pc.getLegalCase().getCnrNumber(), prisoner.getPrison().getName(),
                assessment.getStatus(), assessment.getDaysRemaining(), assessment.getCreditedCustodyDays(),
                pc.getLegalCase().getNextHearingDate());
    }

    private int urgencyScore(LegalAssessment assessment) {
        return switch (assessment.getStatus()) {
            case MAXIMUM_REACHED -> 0;
            case ACTION_OVERDUE -> 1;
            case BAIL_GRANTED -> 2;
            case DATA_INCOMPLETE -> 3;
            case DUE_SOON -> 4;
            default -> 5;
        };
    }

    private long count(Map<EligibilityStatus, Long> counts, EligibilityStatus status) {
        return counts.getOrDefault(status, 0L);
    }

    private int percentage(int value, int base) {
        return base == 0 ? 0 : (int) Math.round((value * 100.0) / base);
    }
}

