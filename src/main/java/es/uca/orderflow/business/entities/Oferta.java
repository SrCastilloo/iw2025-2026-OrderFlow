package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = "productos")
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "oferta")
public class Oferta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 120)
    private String nombre;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private boolean activa = true;

    @Column(name = "descuento_pct", nullable = false, precision = 5, scale = 2)
    private BigDecimal descuentoPct;

    @Enumerated(EnumType.STRING)
    @Column(name = "aplica_a_tipo")
    private ProductoTipo aplicaATipo;

    @Column(name = "aplica_a_todos", nullable = false)
    private boolean aplicaATodos = false;

    private LocalDate fechaInicio;
    private LocalDate fechaFin;

    private LocalTime horaInicio;
    private LocalTime horaFin;

    @Column(name = "dias_semana", length = 32)
    private String diasSemana;

    @Column(nullable = false)
    private int prioridad = 0;

    @ManyToMany
    @JoinTable(
            name = "oferta_producto",
            joinColumns = @JoinColumn(name = "oferta_id"),
            inverseJoinColumns = @JoinColumn(name = "producto_id")
    )
    private Set<Producto> productos = new HashSet<>();
}
