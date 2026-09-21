package in.gov.libertyreckoner.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ProductionSafetyValidatorTest {
    @Test
    void blocksDemonstrationSettingsInProduction() {
        ProductionSafetyValidator validator = validator(new MockEnvironment().withProperty("spring.profiles.active", "prod"));
        assertThatThrownBy(() -> validator.run(new DefaultApplicationArguments()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Production safety checks failed")
                .hasMessageContaining("demonstration data must be disabled");
    }

    @Test
    void allowsDemonstrationSettingsOutsideProduction() {
        ProductionSafetyValidator validator = validator(new MockEnvironment());
        assertThatCode(() -> validator.run(new DefaultApplicationArguments())).doesNotThrowAnyException();
    }

    private ProductionSafetyValidator validator(MockEnvironment environment) {
        ProductionSafetyValidator validator = new ProductionSafetyValidator(environment);
        ReflectionTestUtils.setField(validator, "seedDemoData", true);
        ReflectionTestUtils.setField(validator, "secureCookie", false);
        ReflectionTestUtils.setField(validator, "jwtSecret", "local-development-secret");
        ReflectionTestUtils.setField(validator, "notificationTransport", "log");
        ReflectionTestUtils.setField(validator, "corsOrigins", "http://localhost:8088");
        return validator;
    }
}
