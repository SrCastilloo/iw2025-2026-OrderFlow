package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Empresa;
import es.uca.orderflow.persistence.data.EmpresaRepository;
import org.springframework.stereotype.Service;

@Service
public class EmpresaInfoService {
    private final EmpresaRepository empresaRepository;

    public EmpresaInfoService(EmpresaRepository empresaRepository) {
        this.empresaRepository = empresaRepository;
    }

    /** De momento devolvemos la primera; ajusta a tu lógica real si hay multi-empresa. */
    public Empresa obtenerEmpresaActiva() {
        return empresaRepository.findAll().stream().findFirst().orElse(null);
    }
}
