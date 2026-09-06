package in.gov.libertyreckoner.domain;

import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Entity @Table(name = "prison")
public class Prison {
    @Id @Builder.Default
    private UUID id = UUID.randomUUID();
    @Column(nullable = false, unique = true, length = 30)
    private String code;
    @Column(nullable = false, length = 180)
    private String name;
    @Column(nullable = false, length = 120)
    private String district;
    @Column(nullable = false, length = 120)
    private String state;
    @Column(nullable = false)
    private int capacity;
    @Column(nullable = false)
    private int currentPopulation;
}

