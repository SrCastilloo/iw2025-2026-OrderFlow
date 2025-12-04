package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.RevInfo;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Service
public class ProductoAuditService {

    @PersistenceContext
    private EntityManager em;

    // lo que usas en la vista
    public record ProductoRevision(
            String action,
            LocalDateTime when,
            String who,
            BigDecimal precio,
            Integer stock
    ) {}

    @Transactional(readOnly = true)
    public List<ProductoRevision> historial(Long productoId) {
        AuditReader reader = AuditReaderFactory.get(em);

        @SuppressWarnings("unchecked")
        List<Object[]> rows = reader.createQuery()
                .forRevisionsOfEntity(Producto.class, false, true)
                .add(AuditEntity.id().eq(productoId))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        return rows.stream()
                .map(row -> {
                    Producto p      = (Producto) row[0];
                    RevInfo rev     = (RevInfo) row[1];
                    RevisionType t  = (RevisionType) row[2];

                    String action = switch (t) {
                        case ADD -> "CREADO";
                        case MOD -> "MODIFICADO";
                        case DEL -> "ELIMINADO";
                    };

                    // Fecha/hora desde la tabla revinfo
                    LocalDateTime when = LocalDateTime.ofInstant(
                            Instant.ofEpochMilli(rev.getTimestamp()),
                            ZoneId.systemDefault()
                    );


                    String who = p.getLastModifiedBy();
                    if (who == null || who.isBlank()) {
                        who = p.getCreatedBy();
                    }
                    if (who == null || who.isBlank()) {
                        who = "desconocido";
                    }

                    return new ProductoRevision(
                            action,
                            when,
                            who,
                            p.getPrecio(),
                            p.getStock()
                    );
                })
                .toList();
    }
}
