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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.services.ClienteSesionService;
import es.uca.orderflow.business.services.ModificarCliente;

@PageTitle("Editar perfil")
@Route("/cliente/modificar")
@AnonymousAllowed
public class EditarClienteView extends VerticalLayout {

    public EditarClienteView(ClienteSesionService clienteSesionService,
                             ModificarCliente modificarCliente) {

        // ======== LAYOUT GENERAL ========
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

        H1 title = new H1("Editar mis datos");
        Paragraph subtitle = new Paragraph("Actualiza la información de tu perfil.");

        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.getStyle().set("margin-top", "6vh");
        hero.setAlignItems(Alignment.CENTER);

        // ======== CARD ========
        Div card = new Div();
        card.getStyle()
                .set("width", "min(800px, 94vw)")
                .set("padding", "28px")
                .set("border-radius", "26px")
                .set("background", "rgba(255,255,255,.80)");

        // ======== CLIENTE ========
        Cliente c = clienteSesionService.getActual();
        if (c == null) {
            add(new H2("No se encontró el cliente."));
            return;
        }

        // ======== DIVIDIR DIRECCIÓN SEGURA ========
        String[] p = c.getDireccion().split("\\|\\|");

        String paisVal      = p.length > 0 ? p[0] : "";
        String provinciaVal = p.length > 1 ? p[1] : "";
        String ciudadVal    = p.length > 2 ? p[2] : "";
        String calleVal     = p.length > 3 ? p[3] : "";
        String numeroVal    = p.length > 4 ? p[4] : "";
        String codigoVal    = p.length > 5 ? p[5] : "";

        // ======== CAMPOS EDITABLES ========
        TextField nombre = tf("Nombre", c.getNombre(), VaadinIcon.USER);
        TextField apellidos = tf("Apellidos", c.getApellidos(), VaadinIcon.USER_CARD);
        TextField correo = tf("Correo", c.getCorreo(), VaadinIcon.ENVELOPE);
        TextField telefono = tf("Teléfono", c.getTelefono(), VaadinIcon.PHONE);

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


        // ======== BOTÓN GUARDAR ========
        Button guardar = new Button("Guardar", e -> {
            try {

                // reconstruir dirección con separador seguro
                c.setDireccion(
                        pais.getValue() + "||" +
                                provincia.getValue() + "||" +
                                ciudad.getValue() + "||" +
                                calle.getValue() + "||" +
                                numero.getValue() + "||" +
                                codigoPostal.getValue()
                );

                c.setNombre(nombre.getValue());
                c.setApellidos(apellidos.getValue());
                c.setCorreo(correo.getValue());
                c.setTelefono(telefono.getValue());

                modificarCliente.modificarCliente(c);

                Notification.show("Datos actualizados correctamente.")
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                getUI().ifPresent(ui -> ui.navigate("/cliente/perfil"));

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage())
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        guardar.setIcon(VaadinIcon.CHECK_CIRCLE.create());
        guardar.getStyle().set("width", "240px");

        // ======== BOTÓN CANCELAR ========
        Button cancelar = new Button("Cancelar",
                e -> getUI().ifPresent(ui -> ui.navigate("/cliente/perfil")));

        cancelar.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_LARGE);
        cancelar.setIcon(VaadinIcon.CLOSE.create());
        cancelar.getStyle().set("width", "240px");

        HorizontalLayout botones = new HorizontalLayout(guardar, cancelar);
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
        f.setPrefixComponent(new Icon(icon));
        f.getStyle().set("border-radius", "16px");
        return f;
    }
}
