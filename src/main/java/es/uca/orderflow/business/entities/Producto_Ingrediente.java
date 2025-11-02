package es.uca.orderflow.business.entities;

import es.uca.orderflow.business.entities.Ingrediente;
import es.uca.orderflow.business.entities.Producto;
import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;

@Entity
@Table(name = "producto_ingrediente")
@Data
public class Producto_Ingrediente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // <-- sin cascade
    @JoinColumn(name = "producto_id", nullable = false)
    private Producto producto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false) // <-- sin cascade
    @JoinColumn(name = "ingrediente_id", nullable = false)
    private Ingrediente ingrediente;

    @Column(precision = 10, scale = 3, nullable = false)
    private BigDecimal cantidad;

    @Column(length = 8, nullable = false)
    private String unidad;

}
