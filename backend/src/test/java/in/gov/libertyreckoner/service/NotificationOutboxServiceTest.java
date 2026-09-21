package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.repository.OutboundNotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationOutboxServiceTest {
    @Mock OutboundNotificationRepository notifications;
    @InjectMocks NotificationOutboxService service;

    @BeforeEach
    void configureRecipients() {
        ReflectionTestUtils.setField(service, "superintendentAddress", "superintendent@example.test");
        ReflectionTestUtils.setField(service, "dlsaAddress", "dlsa@example.test");
        ReflectionTestUtils.setField(service, "courtAddress", "court@example.test");
    }

    @Test
    void createsOneDurableAlertForEveryAccountableInstitution() {
        Prisoner prisoner = Prisoner.builder().prisonNumber("UT-101").fullName("Asha Devi").build();
        LegalCase legalCase = LegalCase.builder().cnrNumber("DLCT01-101").build();
        PrisonerCase prisonerCase = PrisonerCase.builder().prisoner(prisoner).legalCase(legalCase).build();
        LegalAssessment assessment = LegalAssessment.builder().prisonerCase(prisonerCase)
                .status(EligibilityStatus.ACTION_OVERDUE).creditedCustodyDays(420)
                .thresholdDays(365).ruleVersion("BNSS-479-v1.0").build();
        when(notifications.existsByEventKey(any())).thenReturn(false);

        service.enqueueEligibilityAlerts(assessment);

        ArgumentCaptor<OutboundNotification> captor = ArgumentCaptor.forClass(OutboundNotification.class);
        verify(notifications, times(3)).save(captor.capture());
        List<UserRole> roles = captor.getAllValues().stream().map(OutboundNotification::getRecipientRole).toList();
        assertThat(roles).containsExactly(UserRole.SUPERINTENDENT, UserRole.DLSA_COUNSEL, UserRole.COURT_REGISTRY);
        assertThat(captor.getAllValues()).allMatch(notification -> notification.getStatus() == NotificationStatus.PENDING);
    }
}
