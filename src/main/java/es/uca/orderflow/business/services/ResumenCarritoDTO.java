package es.uca.orderflow.business.services;

import java.math.BigDecimal;
import java.util.List;

public record ResumenCarritoDTO(List<es.uca.orderflow.business.services.LineaCarritoDTO> items, BigDecimal total) {}
