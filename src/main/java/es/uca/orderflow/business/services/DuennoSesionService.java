package es.uca.orderflow.business.services;

import com.vaadin.flow.server.VaadinSession;
import es.uca.orderflow.business.entities.Duenno;
import org.springframework.stereotype.Service;

@Service
public class DuennoSesionService {

    private static final String KEY = "duenno-actual";

    public void login(Duenno d) {
        VaadinSession.getCurrent().setAttribute(KEY, d);
    }

    public Duenno getActual() {
        VaadinSession s = VaadinSession.getCurrent();
        return s == null ? null : (Duenno) s.getAttribute(KEY);
    }

    public void logout() {
        VaadinSession s = VaadinSession.getCurrent();
        if (s != null) {
            s.setAttribute(KEY, null);
        }
    }
}
