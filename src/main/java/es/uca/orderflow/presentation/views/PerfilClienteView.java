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
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.services.ClienteSesionService;

@PageTitle("Mi perfil")
@Route("/cliente/perfil")
@AnonymousAllowed
public class PerfilClienteView extends VerticalLayout {

    public PerfilClienteView(ClienteSesionService clienteSesionService) {

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
        Paragraph subtitle = new Paragraph("Información personal de tu cuenta.");
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

        // ======== CLIENTE ========
        Cliente cliente = clienteSesionService.getActual();
        if (cliente == null) {
            add(new H2("Error: No se encontró el cliente."));
            return;
        }

        // ======== DIRECCIÓN SEGURA ========
        String[] p = cliente.getDireccion().split("\\|\\|");

        String paisVal      = p.length > 0 ? p[0] : "";
        String provinciaVal = p.length > 1 ? p[1] : "";
        String ciudadVal    = p.length > 2 ? p[2] : "";
        String calleVal     = p.length > 3 ? p[3] : "";
        String numeroVal    = p.length > 4 ? p[4] : "";
        String codigoVal    = p.length > 5 ? p[5] : "";

        // ======== CAMPOS READONLY ========
        TextField nombre = tf("Nombre", cliente.getNombre(), VaadinIcon.USER);
        TextField apellidos = tf("Apellidos", cliente.getApellidos(), VaadinIcon.USER_CARD);
        TextField correo = tf("Correo", cliente.getCorreo(), VaadinIcon.ENVELOPE);
        TextField telefono = tf("Teléfono", cliente.getTelefono(), VaadinIcon.PHONE);

        TextField pais = tf("País", paisVal, VaadinIcon.GLOBE);
        TextField provincia = tf("Provincia", provinciaVal, VaadinIcon.FOLDER_OPEN);
        TextField ciudad = tf("Ciudad", ciudadVal, VaadinIcon.BUILDING);
        TextField calle = tf("Calle", calleVal, VaadinIcon.HOME);
        TextField numero = tf("Número", numeroVal, VaadinIcon.HASH);
        TextField codigoPostal = tf("Código Postal", codigoVal, VaadinIcon.LOCATION_ARROW);

        // ======== FORM LAYOUT ========
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2),
                new FormLayout.ResponsiveStep("1000px", 3)
        );

        form.add(nombre, apellidos, correo, telefono,
                pais, provincia, ciudad, calle, numero, codigoPostal);

        // ======== BOTONES ========
        Button editarBtn = new Button("Modificar mis Datos",
                e -> getUI().ifPresent(ui -> ui.navigate("/cliente/modificar")));
        editarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        editarBtn.setIcon(VaadinIcon.EDIT.create());
        editarBtn.getStyle().set("width", "240px");

        Button volverBtn = new Button("Volver",
                e -> getUI().ifPresent(ui -> ui.navigate("/cliente")));
        volverBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        volverBtn.setIcon(VaadinIcon.ARROW_LEFT.create());
        volverBtn.getStyle().set("width", "240px");

        HorizontalLayout botones = new HorizontalLayout(editarBtn, volverBtn);
        botones.setWidthFull();
        botones.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        // ======== ARMADO ========
        VerticalLayout inner = new VerticalLayout(form, botones);
        inner.setPadding(false);

        card.add(inner);

        add(hero, card);
    }

    private TextField tf(String label, String value, VaadinIcon icon) {
        TextField f = new TextField(label);
        f.setValue(value);
        f.setReadOnly(true);
        f.setPrefixComponent(new Icon(icon));
        f.getStyle().set("border-radius", "16px");
        return f;
    }
}
