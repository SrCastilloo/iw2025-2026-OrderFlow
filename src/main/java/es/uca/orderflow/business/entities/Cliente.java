package es.uca.orderflow.business.entities;

import java.util.Set;

import com.vaadin.flow.component.template.Id;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity //indicamos que es una entidad
@Table(name = "Cliente") //tabla correspondiente en la base de datos
@Data //creación de todos los getters y setters de la clase 
@NoArgsConstructor //permite crear clientes sin argumentos 
@AllArgsConstructor //permite crear clientes con todos los argumentos
public class Cliente {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellidos;
    private String correo;
    private String contrasena;
    private String telefono;
    private String direccion;

    @OneToMany(mappedBy= "cliente", cascade= CascadeType.ALL)
    private Set<Carrito> carritos;

}
