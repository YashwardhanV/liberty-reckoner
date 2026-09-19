package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.service.ApplicationPacketService;
import in.gov.libertyreckoner.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/cases") @RequiredArgsConstructor
public class CaseController {
    private final EligibilityService eligibilityService;
    private final ApplicationPacketService packetService;

    @PostMapping("/{id}/evaluate")
    public AssessmentView evaluate(@PathVariable UUID id) {
        return eligibilityService.evaluate(id);
    }

    @GetMapping("/{id}/application-packet")
    public ApplicationPacketView applicationPacket(@PathVariable UUID id) {
        return packetService.generate(id);
    }
}

