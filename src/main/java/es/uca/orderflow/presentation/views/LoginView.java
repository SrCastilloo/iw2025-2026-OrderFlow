package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
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
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.persistence.data.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

@Route("/login")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;

    @Autowired
    public LoginView(ClienteRepository clienteRepository, PasswordEncoder passwordEncoder) {
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;

        // ======== LAYOUT GLOBAL + PALETA (igual que Registro) ========
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        getStyle().set("background",
                "radial-gradient(1200px 600px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                        "radial-gradient(1000px 500px at 110% 10%, rgba(255,120,90,.35), transparent 60%)," +
                        "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)");
        getElement().getStyle().set("--lumo-primary-color", "hsl(14, 90%, 55%)");
        getElement().getStyle().set("--lumo-primary-text-color", "hsl(14, 90%, 32%)");
        getElement().getStyle().set("--lumo-success-color", "hsl(135, 60%, 38%)");
        getElement().getStyle().set("--lumo-error-color", "hsl(0, 85%, 55%)");
        getElement().getStyle().set("--lumo-border-radius-l", "1.2rem");
        getElement().getStyle().set("--lumo-border-radius-m", "1rem");

        // Sombra animada suave (misma que en Registro, sin ElementFactory.createStyle)
        Element style = new Element("style");
        style.setText("""
          @keyframes softGlow { 
            0% { box-shadow: 0 24px 60px rgba(255, 92, 53, .18); } 
            50% { box-shadow: 0 28px 70px rgba(255, 92, 53, .28);} 
            100% { box-shadow: 0 24px 60px rgba(255, 92, 53, .18);}
          }
        """);
        getElement().appendChild(style);

        // ======== HERO (coherente con Registro) ========
        Icon heroIcon = VaadinIcon.CUTLERY.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 title = new H1("Inicia sesión");
        title.getStyle().set("margin", "0")
                .set("font-size", "clamp(28px, 3vw, 40px)")
                .set("letter-spacing", "-0.02em")
                .set("color", "hsl(14, 90%, 24%)");

        Paragraph subtitle = new Paragraph("Accede a tu cuenta y sigue tu pedido 🍔");
        subtitle.getStyle().set("margin", "6px 0 0 0")
                .set("font-size", "clamp(14px, 2vw, 16px)")
                .set("opacity", "0.85");

        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.setAlignItems(Alignment.CENTER);
        hero.setSpacing(true);
        hero.setPadding(true);
        hero.getStyle().set("margin-top", "6vh").set("margin-bottom", "2vh");

        // ======== CARD/CONTENEDOR (igual que Registro) ========
        Div card = new Div();
        card.getStyle()
                .set("width", "min(640px, 94vw)")
                .set("padding", "28px")
                .set("border-radius", "26px")
                .set("background", "rgba(255,255,255,.78)")
                .set("backdrop-filter", "blur(12px)")
                .set("border", "1px solid rgba(255, 120, 90, .25)")
                .set("animation", "softGlow 6s ease-in-out infinite");

        // ======== FORM (email + password) ========
        FormLayout form = new FormLayout();

        EmailField email = new EmailField("Email");
        email.setPlaceholder("tucorreo@dominio.com");
        email.setClearButtonVisible(true);
        email.setRequired(true);
        email.setPrefixComponent(new Icon(VaadinIcon.ENVELOPE));

        PasswordField password = new PasswordField("Password");
        password.setPlaceholder("Tu contraseña");
        password.setRequired(true);
        password.setPrefixComponent(new Icon(VaadinIcon.LOCK));

        // Validaciones rápidas
        email.addValueChangeListener(e -> {
            String v = e.getValue() == null ? "" : e.getValue().trim();
            boolean ok = v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
            email.setInvalid(!ok);
            if (!ok) email.setErrorMessage("Introduce un correo válido");
        });
        password.addValueChangeListener(e -> {
            boolean ok = e.getValue() != null && !e.getValue().isBlank();
            password.setInvalid(!ok);
            if (!ok) password.setErrorMessage("Campo obligatorio");
        });

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("520px", 2)
        );
        form.add(email, password);
        form.setColspan(email, 2);
        form.setColspan(password, 2);

        // ======== CTA: Acceder ========
        Button acceder = new Button("Acceder");
        acceder.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_SUCCESS);
        acceder.setIcon(VaadinIcon.SIGN_IN.create());
        acceder.setIconAfterText(true);
        acceder.addClickShortcut(Key.ENTER);
        acceder.getStyle()
                .set("width", "100%")
                .set("margin-top", "10px")
                .set("padding", "16px 20px")
                .set("font-weight", "800")
                .set("letter-spacing", ".3px")
                .set("border-radius", "18px")
                .set("background", "linear-gradient(135deg, hsl(14,90%,55%), hsl(10,90%,50%))")
                .set("box-shadow", "0 14px 40px rgba(255, 94, 58, .35)")
                .set("transform-origin", "center");
        acceder.getElement().getStyle().set("transition", "transform .08s ease, box-shadow .2s ease");
        acceder.getElement().addEventListener("mouseenter", e -> {
            acceder.getStyle().set("transform", "translateY(-1px)");
            acceder.getStyle().set("box-shadow", "0 18px 50px rgba(255, 94, 58, .45)");
        });
        acceder.getElement().addEventListener("mouseleave", e -> {
            acceder.getStyle().set("transform", "translateY(0)");
            acceder.getStyle().set("box-shadow", "0 14px 40px rgba(255, 94, 58, .35)");
        });

        // ======== Link a Registro (estilo enlace, como en RegistroView) ========
        Button goRegister = new Button("¿No tienes cuenta? Regístrate");
        goRegister.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        goRegister.getStyle()
                .set("margin-top", "4px")
                .set("color", "hsl(14, 90%, 35%)")
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("text-decoration", "underline")
                .set("cursor", "pointer");
        goRegister.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(RegistroView.class))
        );

        // ======== MONTAJE DE LA CARD ========
        VerticalLayout inner = new VerticalLayout(form, acceder, goRegister);
        inner.setSpacing(false);
        inner.setPadding(false);
        inner.setWidthFull();
        card.add(inner);

        add(hero, card);

        // ======== LÓGICA DE LOGIN ========
        acceder.addClickListener(e -> {
            // Validación mínima en cliente
            if (email.isInvalid() || password.isInvalid()
                    || email.getValue() == null || email.getValue().isBlank()
                    || password.getValue() == null || password.getValue().isBlank()) {
                Notification n = Notification.show("Revisa el email y la contraseña.");
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                n.setPosition(Notification.Position.MIDDLE);
                return;
            }

            acceder.setEnabled(false);
            acceder.setText("Accediendo…");
            acceder.setIcon(VaadinIcon.SPINNER.create());

            try {
                Cliente cliente = clienteRepository.findByCorreo(email.getValue().trim()).orElse(null);
                boolean ok = cliente != null && passwordEncoder.matches(password.getValue(), cliente.getContrasena());

                if (ok) {
                    Notification n = Notification.show("¡Bienvenido, " + cliente.getNombre() + "!");
                    n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    n.setPosition(Notification.Position.MIDDLE);
                    getUI().ifPresent(ui -> ui.navigate("")); // Aqui redirigemos a la pantalla principal
                } else {
                    Notification n = Notification.show("Credenciales incorrectas.");
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    n.setPosition(Notification.Position.MIDDLE);
                }
            } catch (Exception ex) {
                Notification n = Notification.show("Error al iniciar sesión: " + ex.getMessage());
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                n.setPosition(Notification.Position.MIDDLE);
            } finally {
                acceder.setEnabled(true);
                acceder.setText("Acceder");
                acceder.setIcon(VaadinIcon.SIGN_IN.create());
            }
        });
    }
}
