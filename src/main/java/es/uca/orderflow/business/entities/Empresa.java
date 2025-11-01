package es.uca.orderflow.business.entities;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "empresa")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Empresa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombreComercial;


    private String razonSocial;
    private String cif;
    private String correo;
    private String telefono;
    private String direccion1;
    private String direccion2;
    private String ciudad;
    private String provincia;
    private String codigoPostal;
    private String pais;
    private String nombreWeb;
    private String logo; //imagen de la empresa
}
