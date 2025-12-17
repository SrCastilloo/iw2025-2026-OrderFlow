package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.persistence.data.OfertaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OfertaService {

    private final OfertaRepository ofertaRepository;

    public record PrecioInfo(BigDecimal base,
                             BigDecimal finalPrice,
                             BigDecimal descuentoPct,
                             String ofertaNombre) {
        public boolean hayOferta() {
            return descuentoPct != null && descuentoPct.compareTo(BigDecimal.ZERO) > 0;
        }
    }

    /* ===================== LISTADO / CONSULTA ===================== */

    @Transactional(readOnly = true)
    public List<Oferta> listarTodas() {
        // Si añadiste findAllByOrderByIdDesc() úsalo:
        try {
            return ofertaRepository.findAllByOrderByIdDesc();
        } catch (Exception ignore) {
            // fallback si no añades el método
            return ofertaRepository.findAll().stream()
                    .sorted(Comparator.comparing(Oferta::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                    .collect(Collectors.toList());
        }
    }

    @Transactional(readOnly = true)
    public Oferta obtenerPorId(Long id) {
        return ofertaRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("Oferta no encontrada: id=" + id));
    }

    /* ===================== CREAR / ACTUALIZAR ===================== */

    @Transactional
    public Oferta crearOferta(Oferta oferta) {
        validarOferta(oferta);

        // Normaliza productos vs aplicaATodos
        if (oferta.isAplicaATodos()) {
            safeProducts(oferta).clear();
        } else {
            if (safeProducts(oferta).isEmpty()) {
                throw new IllegalArgumentException("Debes seleccionar productos o marcar 'aplica a todos'.");
            }
        }

// Prioridad ya tiene default 0 en la entidad; no puede ser null.
        if (oferta.getPrioridad() < 0) {
            throw new IllegalArgumentException("La prioridad no puede ser negativa.");
        }

        return ofertaRepository.save(oferta);
    }

    @Transactional
    public Oferta actualizarOferta(Long id, Oferta nueva) {
        validarOferta(nueva);

        Oferta o = obtenerPorId(id);

        o.setNombre(nueva.getNombre());
        o.setDescuentoPct(nueva.getDescuentoPct());
        o.setPrioridad(nueva.getPrioridad());
        o.setActiva(nueva.isActiva());

        o.setFechaInicio(nueva.getFechaInicio());
        o.setFechaFin(nueva.getFechaFin());
        o.setHoraInicio(nueva.getHoraInicio());
        o.setHoraFin(nueva.getHoraFin());
        o.setDiasSemana(nueva.getDiasSemana());

        o.setAplicaATodos(nueva.isAplicaATodos());
        o.setAplicaATipo(nueva.getAplicaATipo());

        // Productos
        safeProducts(o).clear();
        if (!o.isAplicaATodos()) {
            safeProducts(o).addAll(safeProducts(nueva));
            if (safeProducts(o).isEmpty()) {
                throw new IllegalArgumentException("Debes seleccionar productos o marcar 'aplica a todos'.");
            }
        }

        return ofertaRepository.save(o);
    }

    /* ===================== ACTIVAR / DESACTIVAR / BORRAR ===================== */

    @Transactional
    public Oferta toggleActiva(Long id) {
        Oferta o = obtenerPorId(id);
        o.setActiva(!o.isActiva());
        return ofertaRepository.save(o);
    }

    @Transactional
    public void eliminarOferta(Long id) {
        if (!ofertaRepository.existsById(id)) {
            throw new NoSuchElementException("Oferta no encontrada: id=" + id);
        }
        ofertaRepository.deleteById(id);
    }

    /* ===================== TU LÓGICA EXISTENTE ===================== */

    @Transactional(readOnly = true)
    public List<Oferta> ofertasVigentesAhora() {
        LocalDateTime now = LocalDateTime.now();
        return ofertaRepository.findByActivaTrue().stream()
                .filter(o -> aplicaAhora(o, now))
                .sorted(Comparator.comparingInt(Oferta::getPrioridad).reversed())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PrecioInfo precioParaProducto(Producto p) {
        if (p == null || p.getPrecio() == null) return new PrecioInfo(BigDecimal.ZERO, BigDecimal.ZERO, null, null);

        BigDecimal base = p.getPrecio();
        LocalDateTime now = LocalDateTime.now();

        Oferta best = ofertaRepository.findByActivaTrue().stream()
                .filter(o -> aplicaAhora(o, now))
                .filter(o -> o.getAplicaATipo() == null || o.getAplicaATipo() == p.getTipo())
                .filter(o -> o.isAplicaATodos() || safeProducts(o).stream().anyMatch(x -> Objects.equals(x.getId(), p.getId())))
                .sorted(Comparator
                        .comparingInt(Oferta::getPrioridad).reversed()
                        .thenComparing(Oferta::getDescuentoPct, Comparator.nullsLast(Comparator.reverseOrder())))
                .findFirst()
                .orElse(null);

        if (best == null) return new PrecioInfo(base, base, null, null);

        BigDecimal pct = best.getDescuentoPct();
        BigDecimal factor = BigDecimal.ONE.subtract(pct.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        BigDecimal fin = base.multiply(factor).setScale(2, RoundingMode.HALF_UP);

        return new PrecioInfo(base, fin, pct, best.getNombre());
    }

    private boolean aplicaAhora(Oferta o, LocalDateTime now) {
        if (o == null || !o.isActiva()) return false;

        LocalDate d = now.toLocalDate();
        LocalTime t = now.toLocalTime();

        if (o.getFechaInicio() != null && d.isBefore(o.getFechaInicio())) return false;
        if (o.getFechaFin() != null && d.isAfter(o.getFechaFin())) return false;

        if (o.getHoraInicio() != null && t.isBefore(o.getHoraInicio())) return false;
        if (o.getHoraFin() != null && t.isAfter(o.getHoraFin())) return false;

        if (o.getDiasSemana() != null && !o.getDiasSemana().isBlank()) {
            Set<DayOfWeek> allowed = parseDias(o.getDiasSemana());
            if (!allowed.contains(now.getDayOfWeek())) return false;
        }

        return true;
    }

    private Set<DayOfWeek> parseDias(String csv) {
        return Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toUpperCase)
                .map(DayOfWeek::valueOf)
                .collect(Collectors.toSet());
    }

    /* ===================== VALIDACIÓN / HELPERS ===================== */

    private void validarOferta(Oferta o) {
        if (o == null) throw new IllegalArgumentException("Oferta nula.");
        if (o.getNombre() == null || o.getNombre().isBlank()) throw new IllegalArgumentException("El nombre es obligatorio.");
        if (o.getDescuentoPct() == null) throw new IllegalArgumentException("El descuento es obligatorio.");
        if (o.getDescuentoPct().compareTo(BigDecimal.ZERO) < 0 || o.getDescuentoPct().compareTo(new BigDecimal("100")) > 0)
            throw new IllegalArgumentException("El descuento debe estar entre 0 y 100.");

        if (o.getFechaInicio() != null && o.getFechaFin() != null && o.getFechaFin().isBefore(o.getFechaInicio()))
            throw new IllegalArgumentException("La fecha fin no puede ser anterior a la fecha inicio.");

        if (o.getHoraInicio() != null && o.getHoraFin() != null && o.getHoraFin().isBefore(o.getHoraInicio()))
            throw new IllegalArgumentException("La hora fin no puede ser anterior a la hora inicio.");

        // diasSemana: se valida “suavemente” (si está mal, falla aquí)
        if (o.getDiasSemana() != null && !o.getDiasSemana().isBlank()) {
            parseDias(o.getDiasSemana()); // lanza IllegalArgumentException si hay tokens inválidos
        }
    }

    private Set<Producto> safeProducts(Oferta o) {
        // Si tu entidad inicializa productos en el constructor, esto no hará falta,
        // pero lo dejo para evitar NPE.
        if (o.getProductos() == null) {
            o.setProductos(new HashSet<>());
        }
        return o.getProductos();
    }
}
