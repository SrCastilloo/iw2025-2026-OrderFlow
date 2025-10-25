package es.uca.orderflow.business.entities;


import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity //indicamos que es una entidad
@Table(name = "cliente") //tabla correspondiente en la base de datos
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

    @OneToOne(mappedBy= "cliente", cascade= CascadeType.ALL)
    private Carrito carrito; //un cliente un carrito, no más.

}
