package es.uca.orderflow.business.entities;


import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.*;

@Entity //indicamos que es una entidad
@Table(name = "cliente") //tabla correspondiente en la base de datos
@NoArgsConstructor //permite crear clientes sin argumentos
@AllArgsConstructor //permite crear clientes con todos los argumentos
@Getter
@Setter
public class Cliente {
    @Id
    @GeneratedValue(strategy= GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellidos;

    @Column(nullable = false, unique =true)
    private String correo;
    
    private String contrasena;
    private String telefono;
    private String direccion;

    @OneToOne(mappedBy= "cliente", cascade= CascadeType.ALL)
    private Carrito carrito; //un cliente un carrito, no más.

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Cliente)) return false;
        Cliente other = (Cliente) o;
        return id != null && id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return 31;
    }

}
