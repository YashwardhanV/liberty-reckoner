package in.gov.libertyreckoner.service;

import in.gov.libertyreckoner.api.ApiDtos.RuleView;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class RuleCatalogueService {
    public List<RuleView> rules() {
        return List.of(
                new RuleView("BNSS-479-1-FT", "First-time offender threshold", "One-third of maximum imprisonment",
                        "Never previously convicted; finite imprisonment; restrictions checked",
                        "Prepare release-on-bond workflow", "BNSS 2023, s.479(1), first proviso"),
                new RuleView("BNSS-479-1", "General undertrial threshold", "One-half of maximum imprisonment",
                        "Previous conviction recorded; finite imprisonment; restrictions checked",
                        "Prepare bail application workflow", "BNSS 2023, s.479(1)"),
                new RuleView("BNSS-479-MAX", "Maximum-period safeguard", "Maximum imprisonment reached",
                        "All cases, subject to verified computation", "Raise critical review; never auto-release",
                        "BNSS 2023, s.479(1), third proviso"),
                new RuleView("BNSS-479-EX", "Death/life exclusion", "No Section 479 fraction",
                        "Death or life is specified as one possible punishment", "Route to other-bail legal review",
                        "BNSS 2023, s.479(1)"),
                new RuleView("BNSS-479-2", "Multiple-offence/case restriction", "Threshold displayed for review",
                        "More than one offence or multiple cases pending", "Do not classify as release-ready",
                        "BNSS 2023, s.479(2)"),
                new RuleView("BNSS-479-3", "Superintendent application duty", "Immediately at threshold",
                        "Verified threshold crossing", "Generate, sign, file and track application",
                        "BNSS 2023, s.479(3)"));
    }
}
