package es.uca.orderflow.business.entities;

import java.util.Set;

import com.vaadin.flow.component.template.Id;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
    @ManyToOne
    @JoinColumn(name="cliente_id",nullable=false)
    private Cliente cliente;

    //relación con Producto (muchos a muchos)
    //DUDA A PREGUNTAR

}
