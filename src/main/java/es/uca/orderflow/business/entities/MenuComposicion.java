package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "menu_composicion",
        uniqueConstraints = @UniqueConstraint(name="uk_menu_producto", columnNames={"menu_producto_id","producto_id"})
)
@Data @NoArgsConstructor @AllArgsConstructor
public class MenuComposicion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // El menú "vendible" (Producto con tipo MENU)
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "menu_producto_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Producto menuProducto;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Producto producto;

    @Column(nullable = false)
    private Integer cantidad = 1;
}
