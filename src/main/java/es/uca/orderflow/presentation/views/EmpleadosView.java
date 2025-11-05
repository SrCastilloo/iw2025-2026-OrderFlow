package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Empleado;
import es.uca.orderflow.persistence.data.EmpleadoRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Gestión de empleados")
@Route("/backoffice/empleados")
@AnonymousAllowed
@CssImport("./styles/empleados.css")
public class EmpleadosView extends VerticalLayout {

    private final EmpleadoRepository empleadoRepository;
    private final VerticalLayout contenedorRoles = new VerticalLayout();

    @Autowired
    public EmpleadosView(EmpleadoRepository empleadoRepository) {
        this.empleadoRepository = empleadoRepository;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        addClassName("backoffice-bg");

        add(crearEncabezado());
        contenedorRoles.addClassName("empleados-page");
        contenedorRoles.setSpacing(false);
        contenedorRoles.setPadding(false);
        add(contenedorRoles);

        recargarEmpleados();
    }

    private Component crearEncabezado() {
        // Título + chips
        Icon heroIcon = VaadinIcon.USERS.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 title = new H1("Gestión de empleados");
        Paragraph subtitle = new Paragraph("Administra tu personal por categoría, crea o elimina empleados.");
        Div titleBox = new Div(title, subtitle);
        titleBox.getElement().getStyle().set("margin-left", "8px");

        HorizontalLayout left = new HorizontalLayout(heroIcon, titleBox);
        left.setAlignItems(Alignment.CENTER);

        // Botón Volver
        Button volver = new Button("Volver", VaadinIcon.ARROW_LEFT.create());
        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volver.addClassName("btn-ghost");
        volver.addClickListener(e -> UI.getCurrent().navigate("/backoffice/duennopanel"));

        // Botón Nuevo
        Button nuevoEmpleado = new Button("Nuevo empleado", VaadinIcon.PLUS.create());
        nuevoEmpleado.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        nuevoEmpleado.addClassName("btn-primary-gradient");
        nuevoEmpleado.addClickListener(e -> UI.getCurrent().navigate("/backoffice/registroempleado"));

        HorizontalLayout right = new HorizontalLayout(volver, nuevoEmpleado);
        right.setAlignItems(Alignment.CENTER);
        right.addClassName("header-actions");

        HorizontalLayout headerBar = new HorizontalLayout(left, right);
        headerBar.addClassName("empleados-header");
        headerBar.setWidth("min(1200px, 92vw)");
        headerBar.setAlignItems(Alignment.CENTER);
        headerBar.setJustifyContentMode(JustifyContentMode.BETWEEN);

        return headerBar;
    }

    private void recargarEmpleados() {
        contenedorRoles.removeAll();

        List<Empleado> empleados = empleadoRepository.findAll();

        if (empleados.isEmpty()) {
            Div vacioBox = new Div(new H3("No hay empleados registrados todavía."));
            vacioBox.addClassName("empty-box");
            contenedorRoles.add(vacioBox);
            return;
        }

        // Agrupar por tipo
        Map<String, List<Empleado>> porTipo = empleados.stream()
                .collect(Collectors.groupingBy(e -> e.getTipoEmpleado().getNombre()));

        porTipo.forEach((tipo, lista) -> {
            Div seccion = new Div();
            seccion.addClassName("empleado-section");

            // Cabecera de sección
            Div chip = new Div();
            chip.addClassName("section-chip");
            chip.setText(" ");

            H3 titulo = new H3(" " + capitalizar(tipo));
            titulo.addClassName("section-title");

            Hr separador = new Hr();
            separador.addClassName("section-hr");

            HorizontalLayout head = new HorizontalLayout(chip, titulo);
            head.setAlignItems(Alignment.CENTER);
            head.setWidthFull();

            // Grid de tarjetas
            Div grid = new Div();
            grid.addClassName("empleado-grid");

            lista.forEach(emp -> grid.add(crearTarjetaEmpleado(emp)));

            seccion.add(head, separador, grid);
            contenedorRoles.add(seccion);
        });
    }

    private Div crearTarjetaEmpleado(Empleado empleado) {
        Div card = new Div();
        card.addClassName("empleado-card");

        // Header
        Div header = new Div();
        header.addClassName("empleado-card__header");

        // Avatar
        String initials = (empleado.getNombre() != null && !empleado.getNombre().isBlank())
                ? empleado.getNombre().trim().substring(0, 1).toUpperCase()
                : "E";
        Div avatar = new Div();
        avatar.addClassName("empleado-avatar");
        avatar.setText(initials);

        H4 name = new H4(empleado.getNombre() + " " + empleado.getApellidos());
        name.addClassName("empleado-name");

        Span rolePill = new Span(empleado.getTipoEmpleado().getNombre());
        rolePill.addClassName("role-pill");

        header.add(avatar, name, rolePill);

        // Meta
        Div meta = new Div();
        meta.addClassName("empleado-meta");

        meta.add(linea("i-mail", empleado.getCorreo()));
        meta.add(linea("i-phone", empleado.getTelefono() != null ? empleado.getTelefono() : "—"));
        meta.add(linea("i-home", empleado.getDireccion() != null ? empleado.getDireccion() : "—"));
        meta.add(linea("i-role", empleado.getTipoEmpleado().getNombre()));

        // Acciones
        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("empleado-actions");

        Button despedir = new Button("Despedir", VaadinIcon.TRASH.create());
        despedir.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        despedir.addClassName("btn-danger-soft");
        despedir.addClickListener(e -> confirmarDespedir(empleado));

        actions.add(despedir);

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

    private void confirmarDespedir(Empleado empleado) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Confirmar despido");
        dlg.add(new Paragraph("¿Seguro que quieres despedir a \"" +
                empleado.getNombre() + " " + empleado.getApellidos() + "\"?"));
        Button cancelar = new Button("Cancelar", e -> dlg.close());
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button confirmar = new Button("Despedir", VaadinIcon.TRASH.create(), e -> {
            empleadoRepository.delete(empleado);
            dlg.close();
            Notification n = Notification.show("Empleado despedido correctamente");
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            recargarEmpleados();
        });
        confirmar.addClassName("btn-danger-solid");
        confirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dlg.getFooter().add(cancelar, confirmar);
        dlg.open();
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) return "";
        return texto.substring(0, 1).toUpperCase() + texto.substring(1).toLowerCase();
    }
}
