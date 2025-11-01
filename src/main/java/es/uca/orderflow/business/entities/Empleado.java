package es.uca.orderflow.business.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empleado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empleado {
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


    @ManyToOne //muchos empleados pueden tener el mismo tipo
    @JoinColumn(name="id_tipo",nullable = false)
    private Tipo_Empleado tipoEmpleado ;

}
