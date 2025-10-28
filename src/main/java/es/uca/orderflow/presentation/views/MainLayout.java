package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.RouterLink;

public class MainLayout extends AppLayout {

    public MainLayout() {
        H1 title = new H1("OrderFlow");
        title.getStyle().set("font-size", "var(--lumo-font-size-l)").set("margin", "0");

        // Enlaza a tus vistas; ajusta clases si cambias nombres
        RouterLink registro = new RouterLink("Registro", RegistroView.class);
        // RouterLink login = new RouterLink("Login", LoginView.class); // cuando lo tengas

        HorizontalLayout header = new HorizontalLayout(title, registro /*, login */);
        header.setWidthFull();
        header.setSpacing(true);
        header.setPadding(true);

        addToNavbar(header);
    }
}
