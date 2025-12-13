package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "caja")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Caja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "opened_at", nullable = false)
    private Date openedAt;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "closed_at")
    private Date closedAt;

    @Column(nullable = false)
    private boolean abierta = true;

    @ManyToOne
    @JoinColumn(name = "opened_by")
    private Empleado openedBy;

    @ManyToOne
    @JoinColumn(name = "closed_by")
    private Empleado closedBy;

    @Column(name = "total_base", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalBase = BigDecimal.ZERO;

    @Column(name = "total_iva", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalIva = BigDecimal.ZERO;

    @Column(name = "total_con_iva", precision = 15, scale = 2, nullable = false)
    private BigDecimal totalConIva = BigDecimal.ZERO;

    @Column(name = "num_pedidos", nullable = false)
    private Integer numPedidos = 0;
}
