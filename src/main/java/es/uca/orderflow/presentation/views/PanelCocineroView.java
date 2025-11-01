package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

@PageTitle("Panel Cocinero")
@Route("/backoffice/cocinero")
@AnonymousAllowed //de momento
public class PanelCocineroView extends VerticalLayout {
    public PanelCocineroView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);
        getStyle().set("background", "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)");
        add(new H2("Panel de Cocinero"));
    }
}
