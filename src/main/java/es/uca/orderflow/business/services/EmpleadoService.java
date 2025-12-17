package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Empleado;
import es.uca.orderflow.persistence.data.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class EmpleadoService {

    @Autowired
    private EmpleadoRepository empleadoRepository;

    public Empleado guardar(Empleado empleado) {
        return empleadoRepository.save(empleado);
    }
}
