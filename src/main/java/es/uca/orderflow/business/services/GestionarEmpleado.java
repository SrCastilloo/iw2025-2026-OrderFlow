package es.uca.orderflow.business.services;


import es.uca.orderflow.business.entities.Empleado;
import es.uca.orderflow.persistence.data.EmpleadoRepository;
import es.uca.orderflow.persistence.data.Tipo_EmpleadoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class GestionarEmpleado {
    private final EmpleadoRepository empleadoRepository;
    private final Tipo_EmpleadoRepository tipo_empleadoRepository;
    private final PasswordEncoder passwordEncoder;


    public GestionarEmpleado(EmpleadoRepository empleadoRepository, Tipo_EmpleadoRepository tipo_empleadoRepository, PasswordEncoder passwordEncoder)
    {
            this.empleadoRepository = empleadoRepository;
            this.tipo_empleadoRepository = tipo_empleadoRepository;
            this.passwordEncoder = passwordEncoder;
    }

    public Empleado CrearEmpleado(Empleado empleado)
    {
        if(empleadoRepository.existsById(empleado.getId()))
            throw new RuntimeException("El empleado ya existe");


        empleado.setContrasena(passwordEncoder.encode(empleado.getContrasena()));

        Empleado creado = empleadoRepository.save(empleado);

        return creado;
    }


    public Empleado modificarEmpleado(Empleado empleado)
    {
        if(!empleadoRepository.existsById(empleado.getId()))
            throw new RuntimeException("El empleado no existe");

        Empleado modificado = empleadoRepository.save(empleado);
        return modificado;
    }

    public Empleado eliminarEmpleado(Empleado empleado)
    {
        if(!empleadoRepository.existsById(empleado.getId()))
                throw new RuntimeException("El empleado no existe");

        Empleado eliminado = empleadoRepository.getOne(empleado.getId());

        empleadoRepository.delete(empleado);

        return eliminado;
    }

    public List<Empleado>  ListarEmpleados()
    {
        return empleadoRepository.findAll();
    }
}
