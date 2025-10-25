package es.uca.orderflow.business.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity // indicamos que es una entidad
@Table(name = "detalle_carrito") // tabla correspondiente en la base de datos
@Data // creación de todos los getters y setters de la clase
@NoArgsConstructor // permite crear carrito sin argumentos
@AllArgsConstructor // permite crear carrito con todos los argumentos
public class Detalle_Carrito {

    // @OneToOne(mappedBy="cliente",cascade= CascadeType.ALL)
    // private Cliente cliente; REDUNDATE. SE OBTIENE DE CARRITO


    //por cada id, tenemos el id del cliente, del carrito,  del producto, junto con su cantidad, precio y subtotal.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "carrito_id", nullable = false)
    private Carrito carrito;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "producto_id", nullable = false,unique = true)
    @ToString.Exclude
    private Producto producto;


    private Integer cantidad;
    private Double precioUnitario;
    private Double subtotal;

}
