package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.theme.lumo.LumoUtility;

import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.services.RegistrarCliente;



//LAS VISTAS PRINCIPALES TENDRÁN @ROUTE JUNTO CON EL PATH PARA LLEGAR A ESA VISTA

@PageTitle("Registro de usuario")
@Route(value = "registro", layout = MainLayout.class)
@AnonymousAllowed
public class RegistroView extends VerticalLayout {

    private final RegistrarCliente registrar;

    public RegistroView(RegistrarCliente registrar) {
        this.registrar = registrar;

        // Canvas
        addClassNames("auth-bg"); // fondo degradado
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // Tarjeta principal
        VerticalLayout card = new VerticalLayout();
        card.addClassNames("auth-card");
        card.setAlignItems(FlexComponent.Alignment.STRETCH);
        card.setSpacing(false);

        // Header de la card (branding)
        HorizontalLayout header = new HorizontalLayout();
        header.addClassNames("auth-header");
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);

        H1 branding = new H1("¡Pide & Listo!");
        branding.addClassNames("auth-brand"); //meterle un estilo en concreto

        Anchor yaCuenta = new Anchor("login", "¿Ya tienes cuenta? Inicia sesión");
        yaCuenta.addClassNames("muted-link");

        header.add(branding, yaCuenta);

        // Título
        Paragraph subtitle = new Paragraph("Crea tu cuenta para pedir tus favoritos en segundos");
        subtitle.addClassNames("auth-subtitle");

        // Formulario
        TextField nombre = new TextField("Nombre");
        nombre.setRequiredIndicatorVisible(true);
        nombre.setPrefixComponent(VaadinIcon.USER.create());

        TextField apellidos = new TextField("Apellidos");
        apellidos.setRequiredIndicatorVisible(true);
        apellidos.setPrefixComponent(VaadinIcon.USER_CARD.create());

        EmailField correo = new EmailField("Correo");
        correo.setRequiredIndicatorVisible(true);
        correo.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        correo.setHelperText("Usaremos tu correo para confirmar pedidos y promociones.");

        TextField telefono = new TextField("Teléfono");
        telefono.setRequiredIndicatorVisible(true);
        telefono.setPrefixComponent(VaadinIcon.PHONE.create());
        telefono.setHelperText("Incluye tu número para contactar con el repartidor.");

        TextArea direccion = new TextArea("Dirección de entrega");
        direccion.setRequiredIndicatorVisible(true);
        direccion.setMaxLength(250);
        direccion.setHeight("100px");
        direccion.setPrefixComponent(VaadinIcon.HOME.create());
        direccion.setHelperText("Ej: Calle Mayor 12, 1ºB, 11001 Cádiz");

        PasswordField contrasena = new PasswordField("Contraseña");
        contrasena.setRequiredIndicatorVisible(true);
        contrasena.setRevealButtonVisible(true);
        contrasena.setPrefixComponent(VaadinIcon.LOCK.create());

        // Medidor de fuerza de contraseña
        ProgressBar strength = new ProgressBar(0, 4, 0);
        strength.setWidthFull();
        strength.addClassNames("pwd-strength");

        Div strengthLabel = new Div(new Text("Fuerza de contraseña"));
        strengthLabel.addClassName("muted");

        contrasena.addValueChangeListener(e -> {
            String v = e.getValue() == null ? "" : e.getValue();
            int score = 0;
            if (v.length() >= 8) score++;
            if (v.matches(".*[A-Z].*")) score++;
            if (v.matches(".*[0-9].*")) score++;
            if (v.matches(".*[^A-Za-z0-9].*")) score++;
            strength.setValue(score);
            strength.getElement().setProperty("theme", score >= 3 ? "success" : (score == 2 ? "contrast" : "error"));
        });

        Checkbox aceptar = new Checkbox("Acepto los términos y condiciones");
        aceptar.setRequiredIndicatorVisible(true);

        Button crear = new Button("Crear cuenta", VaadinIcon.CHECK.create());
        crear.addThemeNames("primary", "large");
        crear.addClassNames("w-full");

        // Binder y validaciones
        Binder<Cliente> binder = new Binder<>(Cliente.class);
        Cliente bean = new Cliente();
        binder.setBean(bean);

        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .withValidator(s -> s.trim().length() >= 2, "Mínimo 2 caracteres")
                .bind(Cliente::getNombre, Cliente::setNombre);

        binder.forField(apellidos)
                .asRequired("Los apellidos son obligatorios")
                .withValidator(s -> s.trim().length() >= 2, "Mínimo 2 caracteres")
                .bind(Cliente::getApellidos, Cliente::setApellidos);

        binder.forField(correo)
                .asRequired("El correo es obligatorio")
                .withValidator(new EmailValidator("Correo inválido"))
                .bind(Cliente::getCorreo, Cliente::setCorreo);

        binder.forField(telefono)
                .asRequired("El teléfono es obligatorio")
                .withValidator(s -> s.replaceAll("\\s", "").matches("^[0-9]{9,15}$"),
                        "Introduce un teléfono válido (9–15 dígitos)")
                .bind(Cliente::getTelefono, Cliente::setTelefono);

        binder.forField(direccion)
                .asRequired("La dirección es obligatoria")
                .withValidator(s -> s.trim().length() >= 6, "Sé un poco más específico")
                .bind(Cliente::getDireccion, Cliente::setDireccion);

        binder.forField(contrasena)
                .asRequired("La contraseña es obligatoria")
                .withValidator(s -> s.length() >= 8, "Mínimo 8 caracteres")
                .bind(Cliente::getContrasena, Cliente::setContrasena);

        // Form responsive (2 columnas en desktop)
        FormLayout form = new FormLayout(nombre, apellidos, correo, telefono, direccion, contrasena);
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2));
        form.setColspan(nombre, 1);
        form.setColspan(apellidos, 1);
        form.setColspan(correo, 1);
        form.setColspan(telefono, 1);
        form.setColspan(direccion, 2);
        form.setColspan(contrasena, 2);

        // Footer
        VerticalLayout footer = new VerticalLayout();
        footer.setSpacing(false);
        footer.setPadding(false);
        footer.add(strengthLabel, strength, aceptar, crear);
        footer.addClassNames(LumoUtility.Gap.SMALL);

        crear.addClickListener(e -> {
            if (!aceptar.getValue()) {
                Notification.show("Debes aceptar los términos y condiciones", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (binder.validate().isOk()) {
                try {
                    registrar.registroCliente(bean);
                    Notification.show("Cuenta creada. ¡Bienvenido/a! 🥳");
                    getUI().ifPresent(ui -> ui.navigate(LoginView.class));
                } catch (Exception ex) {
                    Notification.show("Error: " + ex.getMessage(), 3500, Notification.Position.MIDDLE);
                }
            }
        });

        card.add(header, subtitle, form, footer);
        add(card);
    }
}
