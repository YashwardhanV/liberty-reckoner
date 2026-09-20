package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.service.JusticeIntegrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/integrations/v1") @RequiredArgsConstructor
public class IntegrationController {
    private final JusticeIntegrationService integrationService;

    @PutMapping("/justice-records")
    public IntegrationResult upsert(@Valid @RequestBody JusticeRecordUpsertRequest request) {
        return integrationService.upsert(request);
    }
}
