package in.gov.libertyreckoner.api;

import in.gov.libertyreckoner.api.ApiDtos.DashboardView;
import in.gov.libertyreckoner.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/dashboard") @RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping("/summary")
    public DashboardView summary() {
        return dashboardService.summary();
    }
}

