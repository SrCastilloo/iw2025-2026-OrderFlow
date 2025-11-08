package es.uca.orderflow.business.services;


import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.persistence.data.Duenno_Repository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class GestionarDueno {
    private final Duenno_Repository duennoRepository;
    private final PasswordEncoder passwordEncoder;


    public GestionarDueno(Duenno_Repository repository, PasswordEncoder passwordEncoder)
    {
        this.duennoRepository = repository;
        this.passwordEncoder = passwordEncoder;
    }


    public Duenno crearDuenno(Duenno duenno)
    {
        if(duennoRepository.existsById(duenno.getId()))
            throw new IllegalArgumentException("Este dueño ya existe");


        duenno.setContrasena(passwordEncoder.encode(duenno.getContrasena()));
        return duennoRepository.save(duenno);
    }


    public Duenno modificarDuenno(Duenno duenno)
    {
        if(!duennoRepository.existsById(duenno.getId()))
            throw new IllegalArgumentException("Este duenno no existe");


        return duennoRepository.save(duenno);
    }

    public Duenno eliminarDuenno(Duenno duenno)
    {
        if(!duennoRepository.existsById(duenno.getId()))
            throw new IllegalArgumentException("Este duenno no existe");

        Duenno eliminar =  duennoRepository.findById(duenno.getId()).get();

        duennoRepository.delete(eliminar);
        return eliminar;
    }
}
