package es.uca.orderflow.business.services;

import com.vaadin.flow.spring.annotation.VaadinSessionScope;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.persistence.data.ClienteRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Mantiene el estado del cliente autenticado en la sesión de Vaadin.
 */
@Service
@VaadinSessionScope
public class ClienteSesionService {

    private final IdentificarCliente identificarCliente;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    // id del cliente autenticado (persistido en sesión)
    private Long clienteId;

    public ClienteSesionService(IdentificarCliente identificarCliente,
                                ClienteRepository clienteRepository,
                                PasswordEncoder passwordEncoder) {
        this.identificarCliente = identificarCliente;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /** Autentica y guarda el cliente en sesión. Devuelve el cliente si OK, null si falla. */
    public Cliente login(String email, String rawPassword) {
        Cliente c = identificarCliente.buscaClientePorCorreo(email == null ? "" : email.trim());
        if (c == null) return null;
        if (!passwordEncoder.matches(rawPassword, c.getContrasena())) return null;
        this.clienteId = c.getId();
        return c;
    }

    /** Devuelve el cliente actual o null si no hay sesión. */
    public Cliente getActual() {
        if (clienteId == null) return null;
        Optional<Cliente> c = clienteRepository.findById(clienteId);
        return c.orElse(null);
    }

    /** Devuelve true si hay cliente autenticado en sesión. */
    public boolean isLoggedIn() {
        return clienteId != null && clienteRepository.existsById(clienteId);
    }

    /** Limpia la sesión del cliente. */
    public void logout() {
        this.clienteId = null;
    }


    public Long getClienteId() { return clienteId; }
}
