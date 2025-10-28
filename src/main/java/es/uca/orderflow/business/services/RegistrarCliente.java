package es.uca.orderflow.business.services;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import es.uca.orderflow.business.entities.Carrito;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.persistence.data.CarritoRepository;
import es.uca.orderflow.persistence.data.ClienteRepository;


@Service
public class RegistrarCliente {

    private final CarritoRepository carritoRepository;

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;


    public RegistrarCliente(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder, CarritoRepository carritoRepository)
    {
        this.clienteRepository=clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.carritoRepository = carritoRepository;
    }


    public Cliente registroCliente(Cliente c)
    {
        //primero comprobamos que el correo no existe
        if(clienteRepository.existsByCorreo(c.getCorreo()))
        {
            throw new IllegalArgumentException("Este correo ya existe");
        }

        //guardamos la contraseña cifrada
        c.setContrasena(passwordEncoder.encode(c.getContrasena()));     
        
        Carrito carrito = new Carrito();
        Cliente guardado =clienteRepository.save(c);

        carrito.setCliente(guardado);
        carrito.setPrecio_total(0.);
        carritoRepository.save(carrito);

        return guardado;
    }
}
