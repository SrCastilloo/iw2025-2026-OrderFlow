package es.uca.orderflow.business.services;

import com.vaadin.flow.server.VaadinSession;
import es.uca.orderflow.business.entities.Duenno;
import org.springframework.stereotype.Service;

@Service
public class DuennoSesionService {

    private static final String KEY = "duenno-actual";

    public void login(Duenno d) {
        VaadinSession.getCurrent().setAttribute(KEY, d);
        System.out.println("Dueño logueado: " + d.getNombre());
    }

    public Duenno getActual() {
        VaadinSession s = VaadinSession.getCurrent();
        Duenno duenno = (s == null) ? null : (Duenno) s.getAttribute(KEY);
        System.out.println("Dueño actual: " + (duenno == null ? "Ninguno" : duenno.getNombre()));  // Agregar esta línea
        return duenno;
    }

    public void logout() {
        VaadinSession s = VaadinSession.getCurrent();
        if (s != null) {
            s.setAttribute(KEY, null);
        }
    }
}
