package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.RuleView;
import in.gov.libertyreckoner.service.RuleCatalogueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController @RequestMapping("/api/rules") @RequiredArgsConstructor
public class RuleController {
    private final RuleCatalogueService catalogue;

    @GetMapping
    public List<RuleView> rules() {
        return catalogue.rules();
    }
}

