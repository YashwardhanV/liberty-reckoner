package in.gov.libertyreckoner.repository;

import in.gov.libertyreckoner.domain.NotificationStatus;
import in.gov.libertyreckoner.domain.OutboundNotification;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface OutboundNotificationRepository extends JpaRepository<OutboundNotification, UUID> {
    boolean existsByEventKey(String eventKey);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select notification from OutboundNotification notification
            where notification.status in :statuses and notification.nextAttemptAt <= :now
            order by notification.createdAt asc
            """)
    List<OutboundNotification> findClaimable(@Param("statuses") List<NotificationStatus> statuses,
            @Param("now") Instant now, Pageable pageable);

    @Modifying
    @Query("""
            update OutboundNotification notification
            set notification.status = :failed, notification.nextAttemptAt = :now,
                notification.lockedAt = null, notification.lastError = :reason
            where notification.status = :processing and notification.lockedAt < :staleBefore
            """)
    int recoverStaleClaims(@Param("processing") NotificationStatus processing,
            @Param("failed") NotificationStatus failed, @Param("now") Instant now,
            @Param("staleBefore") Instant staleBefore, @Param("reason") String reason);

    @EntityGraph(attributePaths = {"prisonerCase", "prisonerCase.prisoner"})
    List<OutboundNotification> findTop100ByOrderByCreatedAtDesc();
    @EntityGraph(attributePaths = {"prisonerCase", "prisonerCase.prisoner"})
    List<OutboundNotification> findTop100ByStatusOrderByCreatedAtDesc(NotificationStatus status);
    long countByStatus(NotificationStatus status);
}
