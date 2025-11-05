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

    @Column(name = "nombre_comercial", nullable = false)
    private String nombreComercial;

    @Column(name = "razon_social")
    private String razonSocial;

    @Column(name = "cif")
    private String cif;

    @Column(name = "email_contacto")
    private String correo;

    @Column(name = "telefono_contacto")
    private String telefono;

    @Column(name = "line1")
    private String direccion1;

    @Column(name = "line2")
    private String direccion2;

    @Column(name = "ciudad")
    private String ciudad;

    @Column(name = "provincia")
    private String provincia;

    @Column(name = "cp")
    private String codigoPostal;  // Mapeo a la columna "cp"

    @Column(name = "pais")
    private String pais;

    @Column(name = "web")
    private String nombreWeb;

    @Column(name = "logo_url")
    private String logo; // imagen de la empresa
}
