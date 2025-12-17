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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.services.GestionarDueno;
import es.uca.orderflow.business.services.DuennoSesionService;

@PageTitle("Editar Perfil Dueño")
@Route("/backoffice/duenno/modificardatos")
@AnonymousAllowed
public class EditarPerfilDuennoView extends VerticalLayout {

    public EditarPerfilDuennoView(GestionarDueno gestionarDueno,
                                  DuennoSesionService duennoSesionService) {

        // IMPORTANTE: usar el servicio de sesión correcto
        Duenno duenno = duennoSesionService.getActual();

        if (duenno == null) {
            add(new H2("No se encontró el dueño."));
            return;
        }

        // ======== LAYOUT GENERAL ========
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        getStyle().set("background", "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)");

        // HERO
        Icon heroIcon = VaadinIcon.USER.create();
        heroIcon.getStyle().set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "rgba(255,141,67,.25)");

        H1 title = new H1("Editar mis datos");
        Paragraph subtitle = new Paragraph("Actualiza la información de tu perfil.");

        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.setAlignItems(Alignment.CENTER);
        hero.getStyle().set("margin-top", "6vh");

        // CARD
        Div card = new Div();
        card.getStyle()
                .set("width","min(800px,94vw)")
                .set("padding","28px")
                .set("background","rgba(255,255,255,.80)")
                .set("border-radius","26px");

        // DIRECCIÓN
        String[] p = duenno.getDireccion() != null ?
                duenno.getDireccion().split("\\|\\|") : new String[0];

        String pais      = p.length > 0 ? p[0] : "";
        String provincia = p.length > 1 ? p[1] : "";
        String ciudad    = p.length > 2 ? p[2] : "";
        String calle     = p.length > 3 ? p[3] : "";
        String numero    = p.length > 4 ? p[4] : "";
        String codigo    = p.length > 5 ? p[5] : "";

        // CAMPOS
        TextField nombre = tf("Nombre", duenno.getNombre(), VaadinIcon.USER);
        TextField apellidos = tf("Apellidos", duenno.getApellidos(), VaadinIcon.USER_CARD);
        TextField correo = tf("Correo", duenno.getCorreo(), VaadinIcon.ENVELOPE);
        TextField telefono = tf("Teléfono", duenno.getTelefono(), VaadinIcon.PHONE);

        TextField tfPais = tf("País", pais, VaadinIcon.GLOBE);
        TextField tfProvincia = tf("Provincia", provincia, VaadinIcon.FOLDER_OPEN);
        TextField tfCiudad = tf("Ciudad", ciudad, VaadinIcon.BUILDING);
        TextField tfCalle = tf("Calle", calle, VaadinIcon.HOME);
        TextField tfNumero = tf("Número", numero, VaadinIcon.HASH);
        TextField tfCodigo = tf("Código Postal", codigo, VaadinIcon.LOCATION_ARROW);

        // FORM
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0",1),
                new FormLayout.ResponsiveStep("600px",2),
                new FormLayout.ResponsiveStep("1000px",3)
        );

        form.add(nombre, apellidos, correo, telefono,
                tfPais, tfProvincia, tfCiudad, tfCalle, tfNumero, tfCodigo);

        // BOTÓN GUARDAR
        Button guardar = new Button("Guardar", e -> {

            duenno.setDireccion(
                    tfPais.getValue()+"||"+
                            tfProvincia.getValue()+"||"+
                            tfCiudad.getValue()+"||"+
                            tfCalle.getValue()+"||"+
                            tfNumero.getValue()+"||"+
                            tfCodigo.getValue());

            duenno.setNombre(nombre.getValue());
            duenno.setApellidos(apellidos.getValue());
            duenno.setCorreo(correo.getValue());
            duenno.setTelefono(telefono.getValue());

            gestionarDueno.modificarDuenno(duenno);

            // actualizar sesión
            duennoSesionService.login(duenno);

            Notification.show("Datos actualizados correctamente.")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            getUI().ifPresent(ui -> ui.navigate("/backoffice/perfil"));
        });

        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.setIcon(VaadinIcon.CHECK_CIRCLE.create());

        // BOTÓN CANCELAR
        Button cancelar = new Button("Cancelar",
                e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/perfil")));
        cancelar.addThemeVariants(ButtonVariant.LUMO_ERROR);

        HorizontalLayout botones = new HorizontalLayout(guardar, cancelar);
        botones.setWidthFull();
        botones.setJustifyContentMode(JustifyContentMode.CENTER);

        VerticalLayout inner = new VerticalLayout(form, botones);
        inner.setPadding(false);

        card.add(inner);

        add(hero, card);
    }

    private TextField tf(String label, String value, VaadinIcon icon) {
        TextField f = new TextField(label);
        f.setValue(value != null ? value : "");
        f.setPrefixComponent(new Icon(icon));
        f.getStyle().set("border-radius","16px");
        return f;
    }
}
