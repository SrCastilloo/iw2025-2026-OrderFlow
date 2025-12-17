package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.services.DuennoSesionService;
import es.uca.orderflow.business.services.GestionarMenu;
import es.uca.orderflow.presentation.views.DuennoLoginView;

@PageTitle("Menús")
@Route("/backoffice/menus")
@AnonymousAllowed
public class MenusListView extends VerticalLayout implements BeforeEnterObserver {

    private final GestionarMenu gestionarMenu;
    private final DuennoSesionService duennoSesionService;

    private final Div grid = new Div();

    public MenusListView(GestionarMenu gestionarMenu, DuennoSesionService duennoSesionService) {
        this.gestionarMenu = gestionarMenu;
        this.duennoSesionService = duennoSesionService;

        setSizeFull();
        add(new H2("Menús"));

        Button crear = new Button("Crear menú", VaadinIcon.PLUS.create(),
                e -> UI.getCurrent().navigate("/backoffice/menus/crear"));
        crear.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        grid.getStyle().set("display","grid")
                .set("grid-template-columns","repeat(auto-fit, minmax(320px, 1fr))")
                .set("gap","16px");

        add(crear, grid);
        reload();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (duennoSesionService.getActual() == null) event.forwardTo(DuennoLoginView.class);
    }

    private void reload() {
        grid.removeAll();
        gestionarMenu.listarMenus().forEach(m -> grid.add(menuCard(m)));
    }

    private Component menuCard(Producto menu) {
        Div card = new Div();
        card.getStyle().set("border","1px solid var(--lumo-contrast-10pct)")
                .set("border-radius","14px")
                .set("padding","14px");

        H4 t = new H4(menu.getNombre());
        Span p = new Span(menu.getPrecio() == null ? "—" : menu.getPrecio().toPlainString() + " €");

        Button ver = new Button("Ver composición", e -> UI.getCurrent().navigate("/backoffice/menus/editar/" + menu.getId()));
        Button del = new Button("Eliminar", e -> { gestionarMenu.eliminarMenu(menu.getId()); reload(); });
        del.addThemeVariants(ButtonVariant.LUMO_ERROR);

        card.add(t, p, new HorizontalLayout(ver, del));
        return card;
    }
}
