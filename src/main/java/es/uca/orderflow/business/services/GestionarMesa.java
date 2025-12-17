package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.EstadoMesa;
import es.uca.orderflow.business.entities.Mesa;
import es.uca.orderflow.persistence.data.MesaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GestionarMesa {

    private final MesaRepository mesaRepository;

    @Autowired
    public GestionarMesa(MesaRepository mesaRepository) {
        this.mesaRepository = mesaRepository;
    }

    public Mesa crearMesa(Mesa mesa) {
        return mesaRepository.save(mesa);
    }

    public Mesa guardarMesa(Mesa mesa) {
        return mesaRepository.save(mesa);
    }

    public List<Mesa> obtenerTodasLasMesas() {
        return mesaRepository.findAll();
    }

    public List<Mesa> obtenerMesasLibres() {
        return mesaRepository.findByEstado(EstadoMesa.LIBRE);
    }

    public void eliminarMesa(Mesa mesa) {
        mesaRepository.delete(mesa);
    }

    public void actualizarEstadoMesa(Long mesaId, EstadoMesa nuevoEstado) {
        Mesa mesa = mesaRepository.findById(mesaId)
                .orElseThrow(() -> new IllegalArgumentException("Mesa no encontrada con id " + mesaId));
        mesa.setEstado(nuevoEstado);
        mesaRepository.save(mesa);
    }

    public void marcarMesaOcupada(Long mesaId) {
        actualizarEstadoMesa(mesaId, EstadoMesa.OCUPADA);
    }

    public void marcarMesaLibre(Long mesaId) {
        actualizarEstadoMesa(mesaId, EstadoMesa.LIBRE);
    }
}
