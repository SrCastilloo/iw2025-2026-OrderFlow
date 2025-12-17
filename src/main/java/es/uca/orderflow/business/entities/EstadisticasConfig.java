// src/main/java/es/uca/orderflow/business/entities/EstadisticasConfig.java
package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "estadisticas_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EstadisticasConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private boolean fullUnlocked;

    @Temporal(TemporalType.TIMESTAMP)
    private Date unlockedAt;

    private String unlockedBy;

    // precio del módulo en céntimos (p.ej. 19,90 €)
    private Integer priceCents;
}
