package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.persistence.data.Duenno_Repository;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

@PageTitle("Login dueño")
@Route("/backoffice/duennologin")
@AnonymousAllowed
public class DuennoLoginView extends VerticalLayout {

    private final Duenno_Repository duennoRepository;
    private final PasswordEncoder passwordEncoder;

    private final EmailField correo = new EmailField("Correo");
    private final PasswordField contrasena = new PasswordField("Contraseña");
    private final Binder<LoginDTO> binder = new Binder<>(LoginDTO.class);

    public DuennoLoginView(Duenno_Repository duennoRepository,
                           PasswordEncoder passwordEncoder) {
        this.duennoRepository = duennoRepository;
        this.passwordEncoder = passwordEncoder;

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

        H2 title = new H2("Acceso dueño");
        title.getStyle().set("color", "#1f2937").set("margin", "0");

        Paragraph subtitle = new Paragraph("Inicia sesión con tu correo y contraseña.");
        subtitle.getStyle().set("color", "#6b7280").set("margin", "0 0 16px 0");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        correo.setPlaceholder("duenno@empresa.com");
        correo.setErrorMessage("Correo no válido");
        correo.setPrefixComponent(VaadinIcon.ENVELOPE.create());

        contrasena.setPlaceholder("Tu contraseña");
        contrasena.setPrefixComponent(VaadinIcon.LOCK.create());
        contrasena.setRevealButtonVisible(true);

        form.add(correo, contrasena);

        Button entrar = new Button("Entrar", VaadinIcon.SIGN_IN.create());
        entrar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        entrar.getStyle()
                .set("background", "linear-gradient(90deg,#ff7a59,#ff3d3d)")
                .set("color", "white")
                .set("border-radius", "12px")
                .set("width", "100%");
        entrar.addClickShortcut(Key.ENTER);
        entrar.addClickListener(e -> onLogin());

        Button limpiar = new Button("Limpiar", VaadinIcon.ERASER.create());
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        limpiar.getStyle().set("width", "100%");
        limpiar.addClickShortcut(Key.ESCAPE);
        limpiar.addClickListener(e -> binder.readBean(new LoginDTO()));

        VerticalLayout actions = new VerticalLayout(entrar, limpiar);
        actions.setPadding(false);
        actions.setSpacing(true);

        card.add(title, subtitle, form, actions);
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

        binder.readBean(new LoginDTO());
    }

    private void onLogin() {
        LoginDTO dto = new LoginDTO();
        try {
            binder.writeBean(dto);
        } catch (ValidationException ex) {
            notifyError("Revisa los campos");
            return;
        }

        Optional<Duenno> opt = duennoRepository.findByCorreoIgnoreCase(dto.getCorreo());
        if (opt.isEmpty()) {
            notifyError("No existe un dueño con ese correo");
            return;
        }

        Duenno d = opt.get();
        if (!passwordEncoder.matches(dto.getContrasena(), d.getContrasena())) {
            notifyError("Contraseña incorrecta");
            return;
        }

        Notification.show("Bienvenido, " + d.getNombre(), 2000, Notification.Position.TOP_CENTER)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        // 👉 Redirige al panel de dueño (crea esa vista/ruta)
        getUI().ifPresent(ui -> ui.navigate("/backoffice/duenno/panel"));
    }

    private void notifyError(String msg) {
        Notification n = new Notification();
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.setDuration(3000);
        n.add(new Text(msg));
        n.open();
    }

    public static class LoginDTO {
        private String correo;
        private String contrasena;
        public String getCorreo() { return correo; }
        public void setCorreo(String correo) { this.correo = correo; }
        public String getContrasena() { return contrasena; }
        public void setContrasena(String contrasena) { this.contrasena = contrasena; }
    }
}
