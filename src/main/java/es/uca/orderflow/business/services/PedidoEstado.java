package es.uca.orderflow.business.services;

public enum PedidoEstado {
    PENDIENTE,            //para posible modificación o cancelación
    PREPARACION,     // recién creado
    LISTO_REPARTO,   // cocina lo marca listo
    EN_REPARTO,      // repartidor en ruta
    ENTREGADO,        // finalizado
    CANCELADO          //cancelado por el usuario
}
