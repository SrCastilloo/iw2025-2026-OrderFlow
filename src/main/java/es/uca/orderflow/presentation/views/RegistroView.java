package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.dom.ElementFactory;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.dom.Element;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.services.IdentificarCliente;
import es.uca.orderflow.business.services.RegistrarCliente;
import es.uca.orderflow.persistence.data.ClienteRepository;
import jakarta.persistence.Id;

@Route("/registro")
@AnonymousAllowed
public class RegistroView extends VerticalLayout {

        private final RegistrarCliente  registrarCliente;

    public RegistroView(RegistrarCliente registrarCliente) {
        this.registrarCliente = registrarCliente;
        // ======== LAYOUT GLOBAL + PALETA ========
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

        // ======== CSS EXTRA: confeti + barra superior ========
        Element style = new Element("style");
        style.setText("  @keyframes softGlow { \n" +
                "            0% { box-shadow: 0 24px 60px rgba(255, 92, 53, .18); } \n" +
                "            50% { box-shadow: 0 28px 70px rgba(255, 92, 53, .28);} \n" +
                "            100% { box-shadow: 0 24px 60px rgba(255, 92, 53, .18);}\n" +
                "          }\n" +
                "          /* Barra de progreso fija arriba */\n" +
                "          .top-progress {\n" +
                "            position: fixed;\n" +
                "            top: 0; left: 0; height: 4px; width: 0%;\n" +
                "            z-index: 9999;\n" +
                "            background: linear-gradient(90deg, #ff784e, #ff593a, #ffa76b);\n" +
                "            box-shadow: 0 4px 16px rgba(255, 94, 58, .45);\n" +
                "            transition: width .2s ease;\n" +
                "          }\n" +
                "          .top-progress.loading {\n" +
                "            animation: loadSweep 1.6s ease-out forwards;\n" +
                "          }\n" +
                "          @keyframes loadSweep {\n" +
                "            0% { width: 0%; }\n" +
                "            20% { width: 35%; }\n" +
                "            55% { width: 68%; }\n" +
                "            80% { width: 92%; }\n" +
                "            100% { width: 100%; }\n" +
                "          }\n" +
                "\n" +
                "          /* Confeti */\n" +
                "          .confetti-piece {\n" +
                "            position: fixed;\n" +
                "            top: -10px;\n" +
                "            width: 10px; height: 16px;\n" +
                "            opacity: 0.9;\n" +
                "            transform: translate3d(0,0,0) rotate(0deg);\n" +
                "            border-radius: 2px;\n" +
                "            z-index: 9999;\n" +
                "            animation: fall linear forwards, spin ease-in-out forwards;\n" +
                "          }\n" +
                "          @keyframes fall {\n" +
                "            0% { transform: translateY(-10vh); }\n" +
                "            100% { transform: translateY(110vh); }\n" +
                "          }\n" +
                "          @keyframes spin {\n" +
                "            0% { rotate: 0deg; }\n" +
                "            100% { rotate: 720deg; }\n" +
                "          }");


        getElement().appendChild(style);

        // ======== HERO ========
        Icon heroIcon = VaadinIcon.CUTLERY.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 title = new H1("Registro");
        title.getStyle().set("margin", "0")
                .set("font-size", "clamp(28px, 3vw, 40px)")
                .set("letter-spacing", "-0.02em")
                .set("color", "hsl(14, 90%, 24%)");

        Paragraph subtitle = new Paragraph("Crea tu cuenta y recibe tu comida más rápido que nunca.");
        subtitle.getStyle().set("margin", "6px 0 0 0")
                .set("font-size", "clamp(14px, 2vw, 16px)")
                .set("opacity", "0.85");

        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.setAlignItems(Alignment.CENTER);
        hero.setSpacing(true);
        hero.setPadding(true);
        hero.getStyle().set("margin-top", "6vh").set("margin-bottom", "2vh");

        // ======== CARD / CONTENEDOR ========
        Div card = new Div();
        card.getStyle()
                .set("width", "min(980px, 94vw)")
                .set("padding", "28px")
                .set("border-radius", "26px")
                .set("background", "rgba(255,255,255,.78)")
                .set("backdrop-filter", "blur(12px)")
                .set("border", "1px solid rgba(255, 120, 90, .25)")
                .set("animation", "softGlow 6s ease-in-out infinite");

        // ======== FORM (MISMOS CAMPOS) ========
        FormLayout form = new FormLayout();

        TextField nombre = new TextField("Nombre");
        TextField apellidos = new TextField("Apellidos");
        EmailField email = new EmailField("Email");
        PasswordField password = new PasswordField("Password");
        TextField telefono = new TextField("Telefono");
        TextField calle = new TextField("Calle");
        TextField numero = new TextField("Numero");
        TextField ciudad = new TextField("Ciudad");
        TextField provincia = new TextField("Provincia");
        TextField codigoPostal = new TextField("Código Postal");
        ComboBox<String> pais = new ComboBox<>("País");
        pais.setItems("España","Francia","Portugal","Alemania","Italia");
        pais.setAllowCustomValue(false);

        Button registrar = new Button("Registrarse");

        // Requeridos
        nombre.setRequired(true);    apellidos.setRequired(true);
        email.setRequired(true);     password.setRequired(true);
        telefono.setRequired(true);  calle.setRequired(true);
        numero.setRequired(true);    ciudad.setRequired(true);
        provincia.setRequired(true); codigoPostal.setRequired(true);
        pais.setRequired(true);

        email.setErrorMessage("Introduce un correo válido");
        email.setValue("ejemplo@gmail.com");
        email.setClearButtonVisible(true);

        nombre.setPlaceholder("Tu nombre");
        apellidos.setPlaceholder("Tus apellidos");
        email.setPlaceholder("tucorreo@dominio.com");
        password.setPlaceholder("Mín. 8 caracteres");
        telefono.setPlaceholder("+34 6XX XX XX XX");
        calle.setPlaceholder("Calle Principal");
        numero.setPlaceholder("Ej. 12");
        ciudad.setPlaceholder("Ciudad");
        provincia.setPlaceholder("Provincia");
        codigoPostal.setPlaceholder("Ej. 28013");
        pais.setPlaceholder("Selecciona un país");

        // Iconos
        nombre.setPrefixComponent(new Icon(VaadinIcon.USER));
        apellidos.setPrefixComponent(new Icon(VaadinIcon.USER_CARD));
        email.setPrefixComponent(new Icon(VaadinIcon.ENVELOPE));
        password.setPrefixComponent(new Icon(VaadinIcon.LOCK));
        telefono.setPrefixComponent(new Icon(VaadinIcon.PHONE));
        calle.setPrefixComponent(new Icon(VaadinIcon.HOME));
        numero.setPrefixComponent(new Icon(VaadinIcon.HASH));
        ciudad.setPrefixComponent(new Icon(VaadinIcon.BUILDING));
        provincia.setPrefixComponent(new Icon(VaadinIcon.FOLDER_OPEN));
        codigoPostal.setPrefixComponent(new Icon(VaadinIcon.LOCATION_ARROW));
        pais.setPrefixComponent(new Icon(VaadinIcon.GLOBE));

        // Sufijos OK
        Icon okNombre = VaadinIcon.CHECK_CIRCLE_O.create(); okNombre.setVisible(false); nombre.setSuffixComponent(okNombre);
        Icon okApellidos = VaadinIcon.CHECK_CIRCLE_O.create(); okApellidos.setVisible(false); apellidos.setSuffixComponent(okApellidos);
        Icon okEmail = VaadinIcon.CHECK_CIRCLE_O.create(); okEmail.setVisible(false); email.setSuffixComponent(okEmail);
        Icon okPass = VaadinIcon.CHECK_CIRCLE_O.create(); okPass.setVisible(false); password.setSuffixComponent(okPass);
        Icon okTel = VaadinIcon.CHECK_CIRCLE_O.create(); okTel.setVisible(false); telefono.setSuffixComponent(okTel);
        Icon okCalle = VaadinIcon.CHECK_CIRCLE_O.create(); okCalle.setVisible(false); calle.setSuffixComponent(okCalle);
        Icon okNumero = VaadinIcon.CHECK_CIRCLE_O.create(); okNumero.setVisible(false); numero.setSuffixComponent(okNumero);
        Icon okCiudad = VaadinIcon.CHECK_CIRCLE_O.create(); okCiudad.setVisible(false); ciudad.setSuffixComponent(okCiudad);
        Icon okProvincia = VaadinIcon.CHECK_CIRCLE_O.create(); okProvincia.setVisible(false); provincia.setSuffixComponent(okProvincia);
        Icon okCP = VaadinIcon.CHECK_CIRCLE_O.create(); okCP.setVisible(false); codigoPostal.setSuffixComponent(okCP);
        Icon okPais = VaadinIcon.CHECK_CIRCLE_O.create();
        okPais.setVisible(false);
        okPais.getStyle().set("margin-left", "8px").set("opacity", ".85").set("font-size", "14px");
        pais.setHelperComponent(okPais);

        // Fuerza de contraseña
        ProgressBar passStrength = new ProgressBar(0, 100, 0);
        passStrength.setWidthFull();
        passStrength.getStyle().set("height", "10px").set("border-radius", "999px");
        Paragraph passLabel = new Paragraph("Fuerza de la contraseña");
        passLabel.getStyle().set("font-size", "12px").set("margin", "4px 0 0 0").set("opacity", ".8");
        Div passMeter = new Div(passLabel, passStrength);
        passMeter.getStyle().set("grid-column", "span 3");

        password.addValueChangeListener(e -> {
            String v = e.getValue() == null ? "" : e.getValue();
            int score = 0;
            if (v.length() >= 8) score += 30;
            if (v.matches(".*[A-Z].*")) score += 20;
            if (v.matches(".*[a-z].*")) score += 20;
            if (v.matches(".*\\d.*")) score += 15;
            if (v.matches(".*[^A-Za-z0-9].*")) score += 15;
            if (score > 100) score = 100;
            passStrength.setValue(score);
            if (score < 40) {
                passStrength.getElement().getThemeList().set("error", true);
                passStrength.getElement().getThemeList().remove("success");
                password.setInvalid(true);
                password.setErrorMessage("Contraseña débil (mín. 8, mayúscula, número, símbolo).");
                okPass.setVisible(false);
            } else if (score < 70) {
                passStrength.getElement().getThemeList().remove("error");
                passStrength.getElement().getThemeList().remove("success");
                password.setInvalid(false);
                okPass.setVisible(true);
            } else {
                passStrength.getElement().getThemeList().remove("error");
                passStrength.getElement().getThemeList().set("success", true);
                password.setInvalid(false);
                okPass.setVisible(true);
            }
        });

        // Validaciones rápidas
        email.addValueChangeListener(e -> {
            String v = e.getValue() == null ? "" : e.getValue().trim();
            boolean ok = v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
            email.setInvalid(!ok);
            okEmail.setVisible(ok);
        });
        telefono.addValueChangeListener(e -> {
            String v = e.getValue() == null ? "" : e.getValue().replaceAll("\\s+","");
            boolean ok = v.matches("^\\+?\\d{9,12}$");
            telefono.setInvalid(!ok);
            if (!ok) telefono.setErrorMessage("Formato teléfono: 9-12 dígitos (prefijo opcional).");
            okTel.setVisible(ok);
        });

        Runnable validateCP = () -> {
            String cp = codigoPostal.getValue() == null ? "" : codigoPostal.getValue().trim();
            String p = pais.getValue();
            boolean ok = false;
            if (p == null) {
                ok = cp.length() >= 3;
            } else {
                switch (p) {
                    case "España":   ok = cp.matches("^\\d{5}$"); break;
                    case "Portugal": ok = cp.matches("^\\d{4}-?\\d{3}$"); break;
                    case "Francia":  ok = cp.matches("^\\d{5}$"); break;
                    case "Alemania": ok = cp.matches("^\\d{5}$"); break;
                    case "Italia":   ok = cp.matches("^\\d{5}$"); break;
                    default: ok = cp.length() >= 3;
                }
            }
            codigoPostal.setInvalid(!ok);
            if (!ok) codigoPostal.setErrorMessage("Revisa el código postal para " + (pais.getValue()==null?"el país":pais.getValue()));
            okCP.setVisible(ok);
        };
        codigoPostal.addValueChangeListener(e -> validateCP.run());
        pais.addValueChangeListener(e -> validateCP.run());

        nombre.addValueChangeListener(e -> { boolean ok = !e.getValue().isBlank(); nombre.setInvalid(!ok); okNombre.setVisible(ok); });
        apellidos.addValueChangeListener(e -> { boolean ok = !e.getValue().isBlank(); apellidos.setInvalid(!ok); okApellidos.setVisible(ok); });
        calle.addValueChangeListener(e -> { boolean ok = !e.getValue().isBlank(); calle.setInvalid(!ok); okCalle.setVisible(ok); });
        numero.addValueChangeListener(e -> { boolean ok = !e.getValue().isBlank(); numero.setInvalid(!ok); okNumero.setVisible(ok); });
        ciudad.addValueChangeListener(e -> { boolean ok = !e.getValue().isBlank(); ciudad.setInvalid(!ok); okCiudad.setVisible(ok); });
        provincia.addValueChangeListener(e -> { boolean ok = !e.getValue().isBlank(); provincia.setInvalid(!ok); okProvincia.setVisible(ok); });
        pais.addValueChangeListener(e -> { boolean ok = pais.getValue()!=null; okPais.setVisible(ok); });

        for (var c : new com.vaadin.flow.component.Component[]{
                nombre, apellidos, email, password, telefono,
                calle, numero, ciudad, provincia, codigoPostal, pais
        }) {
            c.getElement().getStyle().set("border-radius", "16px");
            c.getElement().getStyle().set("transition", "box-shadow .2s ease, transform .06s ease");
        }

        // Grid responsive
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("640px", 2),
                new FormLayout.ResponsiveStep("980px", 3)
        );
        form.add(nombre, apellidos, email, password, telefono,
                calle, numero, ciudad, provincia, codigoPostal, pais, passMeter);
        form.setColspan(email, 2);
        form.setColspan(calle, 2);
        form.setColspan(passMeter, 3);

        // CTA
        Button registrarBtn = registrar;
        registrarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_SUCCESS);
        registrarBtn.setIcon(VaadinIcon.FLASH.create());
        registrarBtn.setIconAfterText(true);
        registrarBtn.getStyle()
                .set("width", "100%")
                .set("margin-top", "10px")
                .set("padding", "16px 20px")
                .set("font-weight", "800")
                .set("letter-spacing", ".3px")
                .set("border-radius", "18px")
                .set("background", "linear-gradient(135deg, hsl(14,90%,55%), hsl(10,90%,50%))")
                .set("box-shadow", "0 14px 40px rgba(255, 94, 58, .35)")
                .set("transform-origin", "center");
        registrarBtn.getElement().getStyle().set("transition", "transform .08s ease, box-shadow .2s ease");
        registrarBtn.getElement().addEventListener("mouseenter", e -> {
            registrarBtn.getStyle().set("transform", "translateY(-1px)");
            registrarBtn.getStyle().set("box-shadow", "0 18px 50px rgba(255, 94, 58, .45)");
        });
        registrarBtn.getElement().addEventListener("mouseleave", e -> {
            registrarBtn.getStyle().set("transform", "translateY(0)");
            registrarBtn.getStyle().set("box-shadow", "0 14px 40px rgba(255, 94, 58, .35)");
        });
        registrarBtn.addClickShortcut(Key.ENTER);

        // Barra de progreso superior (hidden por defecto)
        Div topProgress = new Div();
        topProgress.addClassName("top-progress");
        add(topProgress);

        // Footer legal
        Paragraph legal = new Paragraph("Al registrarte aceptas nuestros términos y políticas.");
        legal.getStyle().set("margin", "6px 0 0 0").set("font-size", "12px").set("opacity", ".7");



// Botón tipo enlace: “¿Ya tienes una cuenta?”
        Button loginBtn = new Button("¿Ya tienes una cuenta? Inicia sesión");
        loginBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        loginBtn.getStyle()
                .set("margin-top", "4px")
                .set("color", "hsl(14, 90%, 35%)")
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("text-decoration", "underline")
                .set("cursor", "pointer");

        loginBtn.addClickListener(e ->
                getUI().ifPresent(ui -> ui.navigate(LoginView.class))
        );

        // Montaje
        VerticalLayout inner = new VerticalLayout(form, registrarBtn, legal,loginBtn);
        inner.setSpacing(false);
        inner.setPadding(false);
        inner.setWidthFull();
        card.add(inner);
        add(hero, card);

        // ======== SUBMIT (DEMO con barra + confeti) ========
        registrarBtn.addClickListener(e -> {
            boolean allOk =
                    !nombre.isInvalid() && !apellidos.isInvalid() &&
                            !email.isInvalid() && !password.isInvalid() &&
                            !telefono.isInvalid() && !calle.isInvalid() &&
                            !numero.isInvalid() && !ciudad.isInvalid() &&
                            !provincia.isInvalid() && !codigoPostal.isInvalid() &&
                            pais.getValue()!=null;

            if (!allOk) {
                Notification n = Notification.show("Revisa los campos resaltados.");
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                n.setPosition(Notification.Position.MIDDLE);
                return;
            }

            //aqui llamaria al servicio correspondiente
            Cliente cliente = new Cliente();
            cliente.setNombre(nombre.getValue());
            cliente.setApellidos(apellidos.getValue());
            cliente.setCorreo(email.getValue());
            cliente.setContrasena(password.getValue());
            cliente.setTelefono(telefono.getValue());
            cliente.setDireccion(pais.getValue()+ " "+
                    provincia.getValue() + " " + ciudad.getValue() + " " + calle.getValue() + " " + numero.getValue() +
                    " " + codigoPostal.getValue());


            try {
                cliente = registrarCliente.registroCliente(cliente);






                // Activar barra superior + estado de carga del botón
                topProgress.addClassName("loading");
                registrarBtn.setEnabled(false);
                registrarBtn.setText("Procesando…");
                registrarBtn.setIcon(VaadinIcon.SPINNER.create());
                // Simulación de procesamiento corto y luego éxito
                getUI().ifPresent(ui -> {
                    ui.access(() -> {
                        ui.getPage().executeJs("setTimeout(() => $0.classList.remove('loading'), 1600);", topProgress.getElement());
                    });

                    // Retardo en un hilo aparte y luego actualizamos en el hilo UI
                    new Thread(() -> {
                        try { Thread.sleep(1); } catch (InterruptedException ignored) {}

                        ui.access(() -> {
                            Notification n = Notification.show("¡Cuenta creada! Bienvenido 🍔");
                            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                            n.setPosition(Notification.Position.MIDDLE);

                            fireConfetti();

                            registrarBtn.setEnabled(true);
                            registrarBtn.setText("Registrarse");
                            registrarBtn.setIcon(VaadinIcon.FLASH.create());
                        });
                    }).start();

                });
                Notification.show("¡Registro completado!");

            }catch (IllegalArgumentException ex){
                Notification n = Notification.show(ex.getMessage());
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                n.setPosition(Notification.Position.MIDDLE);

            }catch (Exception ex){
                Notification n = Notification.show(ex.getMessage());
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                n.setPosition(Notification.Position.MIDDLE);
            }





       });
    }

    /** Lanza confeti DOM puro con colores cálidos. */
    private void fireConfetti() {
        getUI().ifPresent(ui -> ui.getPage().executeJs("""
            (function(){
              const colors = ['#ff784e','#ff593a','#ffa76b','#ffd166','#8bd17c','#6ec6ff'];
              const pieces = 120;
              const duration = 1800;
              for (let i=0; i<pieces; i++) {
                const el = document.createElement('div');
                el.className = 'confetti-piece';
                const size = 8 + Math.random()*10;
                el.style.width = size+'px';
                el.style.height = (size*1.4)+'px';
                el.style.left = (Math.random()*100)+'vw';
                el.style.background = colors[Math.floor(Math.random()*colors.length)];
                el.style.animationDuration = (1.1 + Math.random()*0.9) + 's';
                el.style.animationDelay = (Math.random()*0.2)+'s';
                el.style.opacity = (0.75 + Math.random()*0.25);
                el.style.filter = 'saturate('+(0.9 + Math.random()*0.3)+')';
                el.style.transform = 'translateY(-10vh) rotate('+(Math.random()*180)+'deg)';
                document.body.appendChild(el);
                setTimeout(()=>{ if (el && el.parentNode){ el.parentNode.removeChild(el); } }, duration + 500);
              }
            })();
        """));
    }
}
