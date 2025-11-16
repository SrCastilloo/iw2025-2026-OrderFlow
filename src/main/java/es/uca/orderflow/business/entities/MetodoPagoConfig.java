// src/main/java/es/uca/orderflow/business/entities/MetodoPagoConfig.java
package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.*;

import java.util.Date;

@Entity
@Table(name = "metodo_pago_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MetodoPagoConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* banderas */
    private boolean tarjetaEnabled;
    private boolean paypalEnabled;
    private boolean bizumEnabled;
    private boolean transferenciaEnabled;

    /* estado de del pack */
    private boolean fullUnlocked; // si se desbloquearon todos
    @Temporal(TemporalType.TIMESTAMP)
    private Date unlockedAt;
    private String unlockedBy;     // usuario admin/owner

    /* precio del pack en centimos*/
    private Integer priceCents; // p.ej. 4900 = 49.00€
}
