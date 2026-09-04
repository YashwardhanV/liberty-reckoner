package in.gov.libertyreckoner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class LibertyReckonerApplication {
    public static void main(String[] args) {
        SpringApplication.run(LibertyReckonerApplication.class, args);
    }
}

