package es.uca.orderflow.business.services;

import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.persistence.data.ClienteRepository;
import org.springframework.stereotype.Service;

@Service
public class ModificarCliente {
    final private ClienteRepository clienteRepository;

    public ModificarCliente(ClienteRepository clienteRepository) {this.clienteRepository = clienteRepository;}

    public Cliente modificarCliente(Cliente c) {return clienteRepository.save(c);}

    public Cliente ObetenerCliente(long id)  {return clienteRepository.findById(id).orElse(null);}


    }
