package in.gov.libertyreckoner.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.libertyreckoner.domain.AuditEvent;
import in.gov.libertyreckoner.repository.AuditEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import java.util.Map;
import java.util.UUID;

@Service @RequiredArgsConstructor
public class AuditService {
    private final AuditEventRepository repository;
    private final ObjectMapper objectMapper;

    public void record(String aggregateType, UUID aggregateId, String action, Map<String, ?> details) {
        String actor = "system";
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) actor = authentication.getName();
        try {
            repository.save(AuditEvent.builder().aggregateType(aggregateType).aggregateId(aggregateId)
                    .action(action).actor(actor).detailsJson(objectMapper.writeValueAsString(details)).build());
        } catch (Exception exception) {
            throw new IllegalStateException("Could not serialize audit event", exception);
        }
    }
}

