package es.uca.orderflow.business.services;

import com.vaadin.flow.server.VaadinSession;
import es.uca.orderflow.business.entities.Empleado;
import org.springframework.stereotype.Service;

@Service
public class EmpleadoSesionService {

    private static final String KEY = "empleado-actual";

    public void login(Empleado empleado) {
        VaadinSession.getCurrent().setAttribute(KEY, empleado);
    }

    public Empleado getActual() {
        VaadinSession session = VaadinSession.getCurrent();
        return session == null ? null : (Empleado) session.getAttribute(KEY);
    }

    public void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(KEY, null);
        }
    }
}
