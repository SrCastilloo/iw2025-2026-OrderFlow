package es.uca.orderflow.business.entities;

import es.uca.orderflow.business.services.PaymentMethod;
import es.uca.orderflow.business.services.PedidoEstado;
import jakarta.persistence.*;
import lombok.*;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "pedido")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Pedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaRealizacion;

    @Temporal(TemporalType.TIMESTAMP)
    private Date fechaEntrega;

    @ManyToOne
    @JoinColumn(name = "cliente_id", nullable = false)
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Cliente cliente;

    @ManyToOne
    @JoinColumn(name = "repartidor_id") // Nombre de la columna que has añadido en el SQL
    @ToString.Exclude @EqualsAndHashCode.Exclude
    private Empleado repartidor;

    @Column(name = "direccion_envio")
    private String direccionEnvio;

    @OneToMany(mappedBy = "pedido", cascade = CascadeType.ALL, orphanRemoval = true)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private Set<Detalle_Pedido> detallespedido = new HashSet<>();

    // info de pago
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method")
    private PaymentMethod paymentMethod;

    @Column(name = "payment_status")
    private String paymentStatus; // "PAID","FAILED","PENDING"

    @Column(name = "payment_txn_id")
    private String paymentTxnId;

    // estado logístico
    @Enumerated(EnumType.STRING)
    @Column(name = "estado", nullable = false)
    private PedidoEstado estado = PedidoEstado.PREPARACION;
}