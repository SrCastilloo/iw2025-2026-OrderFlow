package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.persistence.data.Duenno_Repository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@PageTitle("Dueños")
@Route("/backoffice/duennos")
@AnonymousAllowed
@CssImport("./styles/empleados.css") // reutilizamos estilos de tarjetas y fondo
public class DuennoView extends VerticalLayout {

    private final Duenno_Repository Duenno_Repository;
    private final VerticalLayout contenido = new VerticalLayout();

    @Autowired
    public DuennoView(Duenno_Repository Duenno_Repository) {
        this.Duenno_Repository = Duenno_Repository;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // Fondo coherente
        addClassName("backoffice-bg");

        add(crearHeader());
        contenido.addClassName("empleados-page"); // mantiene coherencia visual
        contenido.setSpacing(false);
        contenido.setPadding(false);
        add(contenido);

        cargarTarjetas();
    }

    private Component crearHeader() {
        Icon heroIcon = VaadinIcon.USER_STAR.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 title = new H1("Dueños de la empresa");
        Paragraph subtitle = new Paragraph("Listado de propietarios registrados y opciones de edición.");
        Div titleBox = new Div(title, subtitle);
        titleBox.getStyle().set("margin-left", "8px");

        HorizontalLayout left = new HorizontalLayout(heroIcon, titleBox);
        left.setAlignItems(Alignment.CENTER);

        Button volver = new Button("Volver", VaadinIcon.ARROW_LEFT.create());
        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volver.addClassName("btn-ghost");
        volver.addClickListener(e -> UI.getCurrent().navigate("/backoffice/duennopanel"));

        HorizontalLayout right = new HorizontalLayout(volver);
        right.setAlignItems(Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(left, right);
        header.addClassName("empleados-header");
        header.setWidth("min(1200px, 92vw)");
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return header;
    }

    private void cargarTarjetas() {
        contenido.removeAll();

        Div seccion = new Div();
        seccion.addClassName("empleado-section");

        Div chip = new Div();
        chip.addClassName("section-chip");
        H3 titulo = new H3(" Dueños");
        titulo.addClassName("section-title");
        Hr hr = new Hr();
        hr.addClassName("section-hr");

        HorizontalLayout head = new HorizontalLayout(chip, titulo);
        head.setAlignItems(Alignment.CENTER);
        head.setWidthFull();

        Div grid = new Div();
        grid.addClassName("empleado-grid");

        List<Duenno> Duennos = Duenno_Repository.findAll();
        if (Duennos.isEmpty()) {
            Div empty = new Div(new H3("No hay dueños registrados."));
            empty.addClassName("empty-box");
            contenido.add(empty);
            return;
        }

        Duennos.forEach(d -> grid.add(crearTarjetaDuenno(d)));

        seccion.add(head, hr, grid);
        contenido.add(seccion);
    }

    private Div crearTarjetaDuenno(Duenno d) {
        Div card = new Div();
        card.addClassName("empleado-card");

        // Header
        Div header = new Div();
        header.addClassName("empleado-card__header");

        String initials = inicial(d.getNombre());
        Div avatar = new Div();
        avatar.addClassName("empleado-avatar");
        avatar.setText(initials);

        H4 name = new H4((safe(d.getNombre()) + " " + safe(d.getApellidos())).trim());
        name.addClassName("empleado-name");

        Span pill = new Span("Dueño");
        pill.addClassName("role-pill");

        header.add(avatar, name, pill);

        // Meta
        Div meta = new Div();
        meta.addClassName("empleado-meta");
        meta.add(linea("i-mail", safe(d.getCorreo())));
        meta.add(linea("i-phone", safe(d.getTelefono(), "—")));
        meta.add(linea("i-home", safe(d.getDireccion(), "—")));

        // Acciones
        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("empleado-actions");

        Button editar = new Button("Editar", VaadinIcon.EDIT.create());
        editar.addClassName("btn-edit-soft");
        editar.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        editar.addClickListener(e ->
                UI.getCurrent().navigate("/backoffice/Duennos/editar/" + d.getId())
        );

        actions.add(editar);

        card.add(header, meta, actions);
        return card;
    }

    private Div linea(String iconClass, String text) {
        Span icon = new Span();
        icon.addClassName(iconClass);
        Span content = new Span(text);
        Div row = new Div(icon, content);
        row.addClassName("empleado-linea");
        return row;
    }

    private String inicial(String nombre) {
        if (nombre == null || nombre.isBlank()) return "D";
        return nombre.trim().substring(0, 1).toUpperCase();
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String safe(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
