// es.uca.orderflow.audit.UserRevisionListener.java
package es.uca.orderflow.auditoria;

import org.hibernate.envers.RevisionListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.services.DuennoSesionService;

@Component
public class UserRevisionListener implements RevisionListener {

    private static DuennoSesionService staticSesion;

    @Autowired
    public void setSesion(DuennoSesionService sesion) { staticSesion = sesion; }

    @Override
    public void newRevision(Object revisionEntity) {
        AppRevisionEntity rev = (AppRevisionEntity) revisionEntity;
        Duenno d = staticSesion == null ? null : staticSesion.getActual();
        rev.setUser(d == null ? "anon" : d.getCorreo()); // o id
    }
}
