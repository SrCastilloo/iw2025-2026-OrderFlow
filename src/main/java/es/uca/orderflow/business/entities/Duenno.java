package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.checkerframework.checker.units.qual.A;

@Entity
@Table(name="duenno")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Duenno {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre;
    private String apellidos;

    @Column(unique = true,nullable = false)
    private String correo;
    private String contrasena;

    private String telefono;
    private String direccion;
}

