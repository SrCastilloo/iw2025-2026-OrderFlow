package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.services.ClienteSesionService;
import es.uca.orderflow.business.services.ModificarCliente;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Mi perfil") // Se actualizará al abrir la vista
@Route("/cliente/perfil")
@AnonymousAllowed
public class PerfilClienteView extends VerticalLayout {

    private final I18NProvider i18nProvider;
    private final ModificarCliente modificarCliente;

    @Autowired
    public PerfilClienteView(ClienteSesionService clienteSesionService, I18NProvider i18nProvider, ModificarCliente modificarCliente) {
        this.i18nProvider = i18nProvider;
        this.modificarCliente = modificarCliente;

        // Establecer el título de la página traducido al cargar
        setPageTitle(getTranslation("view.profile.title"));

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

        H1 title = new H1(getTranslation("view.profile.title"));
        Paragraph subtitle = new Paragraph(getTranslation("hero.profile.subtitle"));
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
            add(new H2(getTranslation("error.client_not_found")));
            return;
        }

        // ======== DIRECCIÓN SEGURA ========
        String direccion = cliente.getDireccion();
        if (direccion == null || direccion.isEmpty()) {
            direccion = "||||||";
        }

        String[] p = direccion.split("\\|\\|");
        String paisVal = p.length > 0 ? p[0] : "";
        String provinciaVal = p.length > 1 ? p[1] : "";
        String ciudadVal = p.length > 2 ? p[2] : "";
        String calleVal = p.length > 3 ? p[3] : "";
        String numeroVal = p.length > 4 ? p[4] : "";
        String codigoVal = p.length > 5 ? p[5] : "";

        // ======== CAMPOS READONLY ========
        TextField nombre = tf(getTranslation("field.name"), cliente.getNombre(), VaadinIcon.USER);
        TextField apellidos = tf(getTranslation("field.surname"), cliente.getApellidos(), VaadinIcon.USER_CARD);
        TextField correo = tf(getTranslation("field.email"), cliente.getCorreo(), VaadinIcon.ENVELOPE);
        TextField telefono = tf(getTranslation("field.phone"), cliente.getTelefono(), VaadinIcon.PHONE);

        TextField pais = tf(getTranslation("field.country"), paisVal, VaadinIcon.GLOBE);
        TextField provincia = tf(getTranslation("field.province"), provinciaVal, VaadinIcon.FOLDER_OPEN);
        TextField ciudad = tf(getTranslation("field.city"), ciudadVal, VaadinIcon.BUILDING);
        TextField calle = tf(getTranslation("field.street"), calleVal, VaadinIcon.HOME);
        TextField numero = tf(getTranslation("field.number"), numeroVal, VaadinIcon.HASH);
        TextField codigoPostal = tf(getTranslation("field.zip_code"), codigoVal, VaadinIcon.LOCATION_ARROW);

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
        Button eliminarBtn = new Button(getTranslation("button.eliminar"), event -> {
            // Crear un diálogo de confirmación
            Dialog confirmationDialog = new Dialog();
            confirmationDialog.setWidth("300px");
            confirmationDialog.setHeight("200px");

            // Mensaje de confirmación
            Paragraph message = new Paragraph(getTranslation("confirmation.delete_message"));
            Button confirmBtn = new Button(getTranslation("button.confirm"), e -> {
                try {
                    // Llamar al servicio para eliminar al cliente
                    modificarCliente.eliminarCliente(cliente.getId());

                    // Cerrar el diálogo de confirmación
                    confirmationDialog.close();

                    // Mostrar mensaje de éxito
                    Notification.show(getTranslation("notification.account_deleted"), 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                    // Redirigir a la página de login después de eliminar el cliente
                    getUI().ifPresent(ui -> {
                        ui.navigate("/login");
                    });
                } catch (Exception ex) {
                    // En caso de error, mostrar notificación
                    Notification.show(getTranslation("notification.error_delete") + ": " + ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });

            Button cancelBtn = new Button(getTranslation("button.cancel"), e -> {
                confirmationDialog.close(); // Cerrar el diálogo sin hacer nada
            });

            HorizontalLayout actions = new HorizontalLayout(confirmBtn, cancelBtn);
            confirmationDialog.add(message, actions);

            confirmationDialog.open(); // Mostrar el diálogo de confirmación
        });

        eliminarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        eliminarBtn.setIcon(VaadinIcon.PENCIL.create());
        eliminarBtn.getStyle().set("width", "240px");
        eliminarBtn.getStyle().set("background", "#D32F2F");  // Rojo para resaltar
        eliminarBtn.getStyle().set("color", "white");        // Texto en blanco
        eliminarBtn.getStyle().set("font-weight", "bold");   // Texto más grueso
        eliminarBtn.getStyle().set("border-radius", "12px"); // Bordes redondeados
        eliminarBtn.getStyle().set("box-shadow", "0 4px 8px rgba(0,0,0,0.2)"); // Sombra
        eliminarBtn.getStyle().set("transition", "0.3s");   // Transición suave
        eliminarBtn.addClickListener(e -> {
            eliminarBtn.getStyle().set("background", "#C62828");  // Cambio de color al hacer clic
        });

        Button editarBtn = new Button(getTranslation("button.edit_data"),
                e -> getUI().ifPresent(ui -> ui.navigate("/cliente/modificar")));
        editarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        editarBtn.setIcon(VaadinIcon.EDIT.create());
        editarBtn.getStyle().set("width", "240px");

        Button volverBtn = new Button(getTranslation("button.back"),
                e -> getUI().ifPresent(ui -> ui.navigate("/cliente")));
        volverBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_LARGE);
        volverBtn.setIcon(VaadinIcon.ARROW_LEFT.create());
        volverBtn.getStyle().set("width", "240px");

        HorizontalLayout botones = new HorizontalLayout(editarBtn, volverBtn, eliminarBtn);
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
        f.setValue(value != null ? value : "");
        f.setReadOnly(true);
        f.setPrefixComponent(new Icon(icon));
        f.getStyle().set("border-radius", "16px");
        return f;
    }

    // Método para actualizar el PageTitle
    private void setPageTitle(String title) {
        UI.getCurrent().getPage().setTitle(title);
    }
}
