package es.uca.orderflow.business.entities;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name= "tipo_empleado")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Tipo_Empleado {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nombre; //recepcionista, cocinero, repartidor
}
