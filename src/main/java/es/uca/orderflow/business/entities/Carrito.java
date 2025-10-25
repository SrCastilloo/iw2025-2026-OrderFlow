package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.*;
import java.util.HashSet;
import java.util.Set;



@Entity
@Table(name="carrito")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Carrito {

    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id; //identificador del carrito

    //relacion con cliente
    @OneToOne
    @JoinColumn(name="cliente_id",nullable=false,unique=true)
    private Cliente cliente;

    @Column(name="precio_total")
    private double precio_total; //precio total de todos los productos que posee el cliente en el carrito
   

    @OneToMany(mappedBy = "carrito", cascade = CascadeType.ALL)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Set<Detalle_Carrito> detalles = new HashSet<>();



}
