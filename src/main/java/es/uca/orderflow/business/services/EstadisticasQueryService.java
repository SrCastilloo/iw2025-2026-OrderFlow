// src/main/java/es/uca/orderflow/business/services/EstadisticasQueryService.java
package es.uca.orderflow.business.services;

import es.uca.orderflow.business.services.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EstadisticasQueryService {

    @PersistenceContext
    private final EntityManager em;

    public List<TopClienteDTO> topClientes(int limit) {
        return em.createNativeQuery("""
                SELECT c.id AS clienteId, c.nombre AS nombre, COUNT(p.id) AS pedidos
                FROM pedido p
                JOIN cliente c ON c.id = p.cliente_id
                GROUP BY c.id, c.nombre
                ORDER BY pedidos DESC
                LIMIT :lim
                """)
                .setParameter("lim", limit)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("clienteId", Long.class)
                .addScalar("nombre", String.class)
                .addScalar("pedidos", Long.class)
                .setTupleTransformer((tuple, aliases) ->
                        new TopClienteDTO((Long) tuple[0], (String) tuple[1], (Long) tuple[2]))
                .getResultList();
    }

    public List<PedidosMesDTO> pedidosPorMesUltimos12() {
        return em.createNativeQuery("""
                SELECT DATE_FORMAT(p.fecha_realizacion,'%Y-%m') AS periodoYYYYMM, COUNT(*) AS pedidos
                FROM pedido p
                WHERE p.fecha_realizacion IS NOT NULL
                GROUP BY periodoYYYYMM
                ORDER BY periodoYYYYMM DESC
                LIMIT 12
                """)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("periodoYYYYMM", String.class)
                .addScalar("pedidos", Long.class)
                .setTupleTransformer((tuple, aliases) ->
                        new PedidosMesDTO((String) tuple[0], (Long) tuple[1]))
                .getResultList();
    }

    public List<TopProductoDTO> topProductos(int limit) {
        return em.createNativeQuery("""
            SELECT pr.id AS productoId, pr.nombre AS nombre,
                   SUM(dp.cantidad) AS unidades,
                   COALESCE(SUM(dp.importe),0) AS importe
            FROM detalle_pedido dp
            JOIN producto pr ON pr.id = dp.producto_id
            GROUP BY pr.id, pr.nombre
            ORDER BY unidades DESC
            LIMIT :lim
        """)
                .setParameter("lim", limit)
                .unwrap(org.hibernate.query.NativeQuery.class)
                .addScalar("productoId", Long.class)
                .addScalar("nombre", String.class)
                .addScalar("unidades", Long.class)
                .addScalar("importe", Double.class)
                .setTupleTransformer((t, a) -> new TopProductoDTO(
                        (Long) t[0], (String) t[1], ((Long) t[2]), ((Double) t[3])
                )).getResultList();
    }
}
