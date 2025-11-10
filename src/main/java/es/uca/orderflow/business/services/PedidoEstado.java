package es.uca.orderflow.business.services;

public enum PedidoEstado {
    PREPARACION,     // recién creado
    LISTO_REPARTO,   // cocina lo marca listo
    EN_REPARTO,      // repartidor en ruta
    ENTREGADO        // finalizado
}
