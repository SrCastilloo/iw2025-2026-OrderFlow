package es.uca.orderflow.business.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.persistence.data.ClienteRepository;


@Service
public class    IdentificarCliente {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    public IdentificarCliente(ClienteRepository clienteRepository,PasswordEncoder passwordEncoder)
    {
        this.clienteRepository=clienteRepository;
        this.passwordEncoder = passwordEncoder;
        
    }


    public Cliente identificaCliente(String correo, String password)
    {
        Cliente c = clienteRepository.findByCorreo(correo).get();

        //comprobamos que existe el correo del cliente
        if(!clienteRepository.existsByCorreo(correo))
            throw new IllegalArgumentException("Este correo no existe");

        
        //hemos de comprobar la contraseña
        if(!passwordEncoder.matches(password, c.getContrasena()))
            throw new IllegalArgumentException("Contraseña incorrecta");
        


        return c;
    }

}
