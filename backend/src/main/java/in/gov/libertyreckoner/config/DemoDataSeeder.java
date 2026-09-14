package in.gov.libertyreckoner.config;

import in.gov.libertyreckoner.domain.*;
import in.gov.libertyreckoner.repository.*;
import in.gov.libertyreckoner.service.EligibilityService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import java.time.LocalDate;
import java.util.*;

@Component @RequiredArgsConstructor @Order(100)
public class DemoDataSeeder implements CommandLineRunner {
    private final UserAccountRepository users;
    private final PrisonRepository prisons;
    private final PrisonerRepository prisoners;
    private final LegalCaseRepository legalCases;
    private final PrisonerCaseRepository prisonerCases;
    private final PasswordEncoder passwordEncoder;
    private final EligibilityService eligibilityService;

    @Value("${libertyreckoner.seed-demo-data}")
    private boolean enabled;

    @Override
    public void run(String... args) {
        if (!enabled || users.count() > 0) return;
        seedUsers();
        Prison central = prisons.save(Prison.builder().code("DL-TIH-01").name("Central Justice Complex")
                .district("New Delhi").state("Delhi").capacity(920).currentPopulation(1248).build());
        Prison district = prisons.save(Prison.builder().code("DL-RHN-02").name("Rohini District Prison")
                .district("North West Delhi").state("Delhi").capacity(640).currentPopulation(731).build());

        List<UUID> caseIds = new ArrayList<>();
        Prisoner asha = prisoner("UTP-2024-0187", "Asha Devi", Gender.FEMALE, 32, 0, true, "Hindi", central);
        caseIds.add(caseFor(asha, "DLND01-000124-2024", "118/2024", "BNS", "303(2)",
                "Theft", 1095, "3 years", false, false, 500, central, null).getId());

        Prisoner rafiq = prisoner("UTP-2025-0421", "Rafiq Ansari", Gender.MALE, 27, 0, true, "Urdu", central);
        caseIds.add(caseFor(rafiq, "DLND01-000875-2025", "409/2025", "BNS", "318(4)",
                "Cheating and delivery of property", 2555, "7 years", false, false, 820, central, null).getId());

        Prisoner meera = prisoner("UTP-2026-0064", "Meera Solanki", Gender.FEMALE, 41, 1, true, "Hindi", district);
        caseIds.add(caseFor(meera, "DLNW01-000144-2026", "071/2026", "BNS", "303(2)",
                "Theft", 1095, "3 years", false, false, 200, district, null).getId());

        Prisoner salman = prisoner("UTP-2024-0310", "Salman Qureshi", Gender.MALE, 35, 0, true, "Hindi", central);
        caseIds.add(caseFor(salman, "DLND01-000301-2024", "228/2024", "BNS", "303(2)",
                "Theft", 1095, "3 years", false, false, 480, central, null).getId());
        caseIds.add(caseFor(salman, "DLNW01-000702-2024", "512/2024", "BNS", "324(4)",
                "Mischief causing loss", 1825, "5 years", false, false, 420, district, null).getId());

        Prisoner dinesh = prisoner("UTP-2025-0119", "Dinesh Pal", Gender.MALE, 38, 0, true, "Hindi", district);
        caseIds.add(caseFor(dinesh, "DLNW01-000433-2025", "201/2025", "BNS", "109",
                "Attempt to murder", 3650, "10 years or life", false, true, 430, district, null).getId());

        Prisoner kavita = prisoner("UTP-2025-0598", "Kavita Rao", Gender.FEMALE, 29, 0, false, "Marathi", central);
        caseIds.add(caseFor(kavita, "DLND01-000988-2025", "660/2025", "BNS", "303(2)",
                "Theft", 1095, "3 years", false, false, 390, central, null).getId());

        Prisoner arjun = prisoner("UTP-2024-0733", "Arjun Yadav", Gender.MALE, 23, 0, true, "Hindi", central);
        caseIds.add(caseFor(arjun, "DLND01-001242-2024", "821/2024", "BNS", "303(2)",
                "Theft", 1095, "3 years", false, false, 610, central, LocalDate.now().minusDays(4)).getId());

        caseIds.forEach(eligibilityService::evaluate);
    }

    private void seedUsers() {
        String password = passwordEncoder.encode("Liberty@123");
        saveUser("System Administrator", "admin@libertyreckoner.gov.in", UserRole.ADMIN, password);
        saveUser("Ananya Sen", "superintendent@libertyreckoner.gov.in", UserRole.SUPERINTENDENT, password);
        saveUser("Kabir Mehta", "dlsa@libertyreckoner.gov.in", UserRole.DLSA_COUNSEL, password);
        saveUser("Court Registry Desk", "registry@libertyreckoner.gov.in", UserRole.COURT_REGISTRY, password);
        saveUser("Independent Auditor", "auditor@libertyreckoner.gov.in", UserRole.AUDITOR, password);
    }

    private void saveUser(String name, String email, UserRole role, String password) {
        users.save(UserAccount.builder().fullName(name).email(email).role(role).passwordHash(password).build());
    }

    private Prisoner prisoner(String number, String name, Gender gender, int age, int convictions,
            boolean verified, String language, Prison prison) {
        return prisoners.save(Prisoner.builder().prisonNumber(number).fullName(name).gender(gender)
                .dateOfBirth(LocalDate.now().minusYears(age)).nationality("Indian").preferredLanguage(language)
                .previousConvictions(convictions).convictionHistoryVerified(verified).prison(prison).build());
    }

    private PrisonerCase caseFor(Prisoner prisoner, String cnr, String fir, String act, String section,
            String description, int maxDays, String maxLabel, boolean death, boolean life, int custodyDays,
            Prison prison, LocalDate bailGranted) {
        LegalCase legalCase = legalCases.save(LegalCase.builder().cnrNumber(cnr).firNumber(fir)
                .policeStation(prison.getDistrict() + " Police Station").courtName("District & Sessions Court, " + prison.getDistrict())
                .district(prison.getDistrict()).state(prison.getState()).stage(CaseStage.TRIAL)
                .chargeSheetDate(LocalDate.now().minusDays(Math.max(30, custodyDays - 45)))
                .nextHearingDate(LocalDate.now().plusDays(5 + Math.abs(cnr.hashCode() % 18))).build());
        PrisonerCase pc = PrisonerCase.builder().prisoner(prisoner).legalCase(legalCase)
                .bailGrantedDate(bailGranted).build();
        pc.getCharges().add(Charge.builder().prisonerCase(pc).actName(act).sectionCode(section)
                .description(description).maximumTermDays(maxDays).maximumTermLabel(maxLabel)
                .deathPunishmentPossible(death).lifeImprisonmentPossible(life).effectiveFrom(LocalDate.of(2024, 7, 1))
                .legalSource("Demonstration catalogue — legal validation required").build());
        pc.getCustodyPeriods().add(CustodyPeriod.builder().prisonerCase(pc)
                .startDate(LocalDate.now().minusDays(custodyDays - 1L)).sourceSystem("e-Prisons demo adapter").build());
        return prisonerCases.save(pc);
    }
}
