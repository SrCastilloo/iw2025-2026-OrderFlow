package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import es.uca.orderflow.business.entities.Empleado;
import es.uca.orderflow.persistence.data.EmpleadoRepository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Locale;
import java.util.Optional;

@PageTitle("Login empleado")
@Route("/backoffice/empleadologin")
@AnonymousAllowed
public class EmpleadoLoginView extends VerticalLayout {

    private final EmpleadoRepository empleadoRepository;
    private final PasswordEncoder passwordEncoder;


    // UI
    private final EmailField correo = new EmailField("Correo");
    private final PasswordField contrasena = new PasswordField("Contraseña");
    private final Binder<LoginDTO> binder = new Binder<>(LoginDTO.class);

    public EmpleadoLoginView(EmpleadoRepository empleadoRepository, PasswordEncoder passwordEncoder) {
        this.empleadoRepository = empleadoRepository;
        this.passwordEncoder = passwordEncoder;

        // Fondo claro coherente con el registro
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        getStyle().set("background", "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)");

        add(buildCard());
        configureBinding();
    }

    private Div buildCard() {
        Div card = new Div();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "24px")
                .set("box-shadow", "0 20px 60px rgba(0,0,0,0.10)")
                .set("padding", "28px")
                .set("margin", "48px 32px")
                .set("max-width", "560px")
                .set("width", "92%")
                .set("border", "1px solid #f1f1f1");

        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon badge = VaadinIcon.USER_CHECK.create();
        badge.setColor("#ff7a59");
        badge.setSize("36px");

        VerticalLayout titleBox = new VerticalLayout();
        titleBox.setPadding(false);
        titleBox.setSpacing(false);
        H2 title = new H2("Acceso empleados");
        title.getStyle().set("color", "#1f2937").set("margin", "0");
        Paragraph subtitle = new Paragraph("Inicia sesión con tu correo corporativo.");
        subtitle.getStyle().set("color", "#6b7280").set("margin", "0 0 8px 0");
        titleBox.add(title, subtitle);

        header.add(badge, titleBox);
        header.expand(titleBox);

        // Form
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        correo.setPlaceholder("ejemplo@empresa.com");
        correo.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        correo.setClearButtonVisible(true);
        correo.setErrorMessage("Correo no válido");

        contrasena.setPlaceholder("Tu contraseña");
        contrasena.setPrefixComponent(VaadinIcon.LOCK.create());
        contrasena.setRevealButtonVisible(true);

        form.add(correo, contrasena);

        // Buttons
        Button entrar = new Button("Entrar", VaadinIcon.SIGN_IN.create());
        entrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        entrar.getStyle()
                .set("background", "linear-gradient(90deg,#ff7a59,#ff3d3d)")
                .set("color", "white")
                .set("border-radius", "12px")
                .set("width", "100%");

        Button limpiar = new Button("Limpiar", VaadinIcon.ERASER.create());
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        limpiar.getStyle().set("width", "100%");

        entrar.addClickShortcut(Key.ENTER);
        limpiar.addClickShortcut(Key.ESCAPE);

        entrar.addClickListener(e -> onLogin());
        limpiar.addClickListener(e -> binder.readBean(new LoginDTO()));

        VerticalLayout actions = new VerticalLayout(entrar, limpiar);
        actions.setPadding(false);
        actions.setSpacing(true);

        card.add(header, form, actions);
        return card;
    }

    private void configureBinding() {
        binder.forField(correo)
                .asRequired("El correo es obligatorio")
                .withValidator(new EmailValidator("Correo no válido"))
                .bind(LoginDTO::getCorreo, LoginDTO::setCorreo);

        binder.forField(contrasena)
                .asRequired("La contraseña es obligatoria")
                .withValidator(pw -> pw != null && !pw.isBlank(), "Introduce tu contraseña")
                .bind(LoginDTO::getContrasena, LoginDTO::setContrasena);

        // Estado inicial
        binder.readBean(new LoginDTO());
    }

    private void onLogin() {
        LoginDTO dto = new LoginDTO();
        try {
            binder.writeBean(dto);
        } catch (ValidationException ex) {
            Notification.show("Revisa los campos", 2500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Optional<Empleado> opt = empleadoRepository.findByCorreoIgnoreCase(dto.getCorreo());
        if (opt.isEmpty()) {
            error("No existe un empleado con ese correo.");
            return;
        }

        Empleado emp = opt.get();

        // ⚠️ Cambia esta comparación si usas PasswordEncoder:
        // if (!passwordEncoder.matches(dto.getContrasena(), emp.getContrasena()))
        if (!passwordEncoder.matches(dto.getContrasena(), emp.getContrasena())) {
            error("Contraseña incorrecta.");
            return;
        }

        // Redirección según tipo
        String tipo = (emp.getTipoEmpleado() != null && emp.getTipoEmpleado().getNombre() != null)
                ? emp.getTipoEmpleado().getNombre().trim().toLowerCase(Locale.ROOT)
                : "";

        switch (tipo) {
            case "recepcionista" -> getUI().ifPresent(ui -> ui.navigate(PanelRecepcionistaView.class));
            case "cocinero"      -> getUI().ifPresent(ui -> ui.navigate(PanelCocineroView.class));
            case "repartidor"    -> getUI().ifPresent(ui -> ui.navigate(PanelRepartidorView.class));
            default -> {
                Notification.show("Tipo de empleado no reconocido: " + tipo, 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        }
    }

    private void error(String msg) {
        Notification n = new Notification();
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.setDuration(3000);
        n.add(new Text(msg));
        n.open();
    }

    // DTO sencillo para el binder
    public static class LoginDTO {
        private String correo;
        private String contrasena;
        public String getCorreo() { return correo; }
        public void setCorreo(String correo) { this.correo = correo; }
        public String getContrasena() { return contrasena; }
        public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    }
}
