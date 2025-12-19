package es.uca.orderflow.presentation.dto;

import es.uca.orderflow.business.entities.ProductoTipo;
import java.math.BigDecimal;

public record ProductoCardDTO(
        Long id,
        String nombre,
        String descripcion,
        BigDecimal precio,
        String foto,
        ProductoTipo tipo
) {}
