package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Empleado;

@PageTitle("Mi Perfil")
@Route("/backoffice/empleado/perfil")
@AnonymousAllowed
public class PerfilEmpleadoView extends VerticalLayout {

    public PerfilEmpleadoView() {

        // ======== EMPLEADO DE SESIÓN (VERSIÓN REAL DEL PROYECTO) ========
        Empleado empleado = (Empleado) VaadinSession.getCurrent().getAttribute("empleadoLogueado");

        if (empleado == null) {
            add(new H2("Error: No se encontró el empleado."));
            return;
        }

        // ======== LAYOUT GLOBAL ========
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        getStyle().set("background",
                "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)");

        // ======== HERO ========
        Icon heroIcon = VaadinIcon.USER.create();
        heroIcon.getStyle().set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "rgba(255,141,67,.25)");

        H1 title = new H1("Mi Perfil");
        Paragraph subtitle = new Paragraph("Información personal del empleado.");
        subtitle.getStyle().set("margin", "6px 0 0 0");

        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.setAlignItems(Alignment.CENTER);
        hero.getStyle().set("margin-top", "6vh");

        // ======== CARD ========
        Div card = new Div();
        card.getStyle()
                .set("width", "min(800px, 94vw)")
                .set("padding", "28px")
                .set("border-radius", "26px")
                .set("background", "rgba(255,255,255,.80)");

        // ======== DIRECCIÓN ========
        String[] p = empleado.getDireccion() != null ?
                empleado.getDireccion().split("\\|\\|") : new String[0];

        String pais      = p.length > 0 ? p[0] : "";
        String provincia = p.length > 1 ? p[1] : "";
        String ciudad    = p.length > 2 ? p[2] : "";
        String calle     = p.length > 3 ? p[3] : "";
        String numero    = p.length > 4 ? p[4] : "";
        String codigo    = p.length > 5 ? p[5] : "";

        // ======== CAMPOS ========
        TextField nombre = tf("Nombre", empleado.getNombre(), VaadinIcon.USER);
        TextField apellidos = tf("Apellidos", empleado.getApellidos(), VaadinIcon.USER_CARD);
        TextField correo = tf("Correo", empleado.getCorreo(), VaadinIcon.ENVELOPE);
        TextField telefono = tf("Teléfono", empleado.getTelefono(), VaadinIcon.PHONE);

        TextField tipo = tf("Tipo de empleado",
                empleado.getTipoEmpleado() != null ? empleado.getTipoEmpleado().getNombre() : "",
                VaadinIcon.USER_STAR);

        TextField tfPais = tf("País", pais, VaadinIcon.GLOBE);
        TextField tfProvincia = tf("Provincia", provincia, VaadinIcon.FOLDER_OPEN);
        TextField tfCiudad = tf("Ciudad", ciudad, VaadinIcon.BUILDING);
        TextField tfCalle = tf("Calle", calle, VaadinIcon.HOME);
        TextField tfNumero = tf("Número", numero, VaadinIcon.HASH);
        TextField tfCodigo = tf("Código Postal", codigo, VaadinIcon.LOCATION_ARROW);

        // ======== FORM ========
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        form.add(nombre, apellidos, correo, telefono, tipo,
                tfPais, tfProvincia, tfCiudad, tfCalle, tfNumero, tfCodigo);

        // ======== BOTONES ========
        Button editarBtn = new Button("Modificar mis datos",
                e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/empleado/modificar")));
        editarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button volverBtn = new Button("Volver",
                e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/recepcionista")));
        volverBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

        HorizontalLayout botones = new HorizontalLayout(editarBtn, volverBtn);
        botones.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        botones.setWidthFull();

        VerticalLayout inner = new VerticalLayout(form, botones);
        inner.setPadding(false);

        card.add(inner);

        add(hero, card);
    }

    private TextField tf(String label, String value, VaadinIcon icon) {
        TextField f = new TextField(label);
        f.setValue(value != null ? value : "");
        f.setReadOnly(true);
        f.setPrefixComponent(new Icon(icon));
        f.getStyle().set("border-radius", "16px");
        return f;
    }
}
