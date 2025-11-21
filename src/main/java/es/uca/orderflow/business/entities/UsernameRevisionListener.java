package es.uca.orderflow.business.entities;

import org.hibernate.envers.RevisionListener;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class UsernameRevisionListener implements RevisionListener {

    @Override
    public void newRevision(Object revisionEntity) {
        RevInfo rev = (RevInfo) revisionEntity;

        String username = "desconocido";

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken)) {
            username = auth.getName();  //usuario
        }

        rev.setUser(username);
    }
}
