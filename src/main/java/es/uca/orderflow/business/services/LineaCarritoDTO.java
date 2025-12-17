package es.uca.orderflow.business.services;

import java.math.BigDecimal;
import java.util.List;

public record LineaCarritoDTO(Long productoId, String nombre, String foto,
                              int cantidad, BigDecimal precioUnitario, BigDecimal subtotal) {}

