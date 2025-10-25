package es.uca.orderflow.business.entities;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.persistence.Id;


@Entity
@Table(name= "detalle_pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Detalle_Pedido {   

    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;



    //relación con pedido
    @ManyToOne
    @JoinColumn(name = "pedido_id",nullable=false)
    private Pedido pedido; 


    //relación con los productos que contiene
    @ManyToOne
    @JoinColumn(name = "producto_id",nullable=false)
    private Producto producto;

    private double importe;
    private double precioUnitario;
    private Integer cantidad;


}
