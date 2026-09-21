package in.gov.libertyreckoner;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "libertyreckoner.seed-demo-data=false",
        "libertyreckoner.notifications.dispatch-enabled=false"
})
class LibertyReckonerApplicationIntegrationTest {
    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("libertyreckoner_test")
            .withUsername("libertyreckoner")
            .withPassword("integration-test-password");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void startsAgainstPostgresAndAppliesEveryMigration() {
        Integer migrations = jdbc.queryForObject(
                "select count(*) from flyway_schema_history where success = true", Integer.class);
        String outboxTable = jdbc.queryForObject("select to_regclass('public.outbound_notification')", String.class);
        String assessmentTable = jdbc.queryForObject("select to_regclass('public.legal_assessment')", String.class);

        assertThat(migrations).isGreaterThanOrEqualTo(3);
        assertThat(outboxTable).isEqualTo("outbound_notification");
        assertThat(assessmentTable).isEqualTo("legal_assessment");
    }
}
