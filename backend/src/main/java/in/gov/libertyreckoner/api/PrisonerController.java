package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.*;
import in.gov.libertyreckoner.service.PrisonerService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.UUID;

@RestController @RequestMapping("/api/prisoners") @RequiredArgsConstructor
public class PrisonerController {
    private final PrisonerService prisonerService;

    @GetMapping
    public PageResponse<PrisonerListView> list(
            @RequestParam(defaultValue = "") String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return prisonerService.list(query, page, size);
    }

    @GetMapping("/{id}")
    public PrisonerView detail(@PathVariable UUID id) {
        return prisonerService.detail(id);
    }
}

