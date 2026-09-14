package in.gov.libertyreckoner.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ProductionSafetyValidator implements ApplicationRunner {
    private final Environment environment;

    @Value("${libertyreckoner.seed-demo-data}") private boolean seedDemoData;
    @Value("${libertyreckoner.jwt.secure-cookie}") private boolean secureCookie;
    @Value("${libertyreckoner.jwt.secret}") private String jwtSecret;
    @Value("${libertyreckoner.notifications.transport}") private String notificationTransport;
    @Value("${libertyreckoner.cors-allowed-origins}") private String corsOrigins;

    @Override
    public void run(ApplicationArguments arguments) {
        if (!environment.acceptsProfiles(Profiles.of("prod"))) return;
        List<String> violations = new ArrayList<>();
        if (seedDemoData) violations.add("demonstration data must be disabled");
        if (!secureCookie) violations.add("secure session cookies must be enabled");
        if (jwtSecret.startsWith("local-development")) violations.add("the development JWT secret must be replaced");
        if ("log".equalsIgnoreCase(notificationTransport)) violations.add("a real notification transport must be configured");
        if (corsOrigins.contains("localhost")) violations.add("CORS must not allow localhost");
        if (!violations.isEmpty()) {
            throw new IllegalStateException("Production safety checks failed: " + String.join("; ", violations));
        }
    }
}
