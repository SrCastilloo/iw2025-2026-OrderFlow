package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "mesa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Mesa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre; // Nombre de la mesa

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoMesa estado; // Estado de la mesa

    @Column(nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime fechaCreacion; // Fecha de creación
}
