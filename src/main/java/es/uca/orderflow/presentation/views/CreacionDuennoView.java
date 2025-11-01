package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.persistence.data.Duenno_Repository;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.regex.Pattern;

@PageTitle("Registro de dueño")
@Route("/backoffice/registroduenno")
@AnonymousAllowed
public class CreacionDuennoView extends VerticalLayout {

    private final Duenno_Repository duennoRepository;
    private final PasswordEncoder passwordEncoder;

    // Campos
    private final TextField nombre = new TextField("Nombre");
    private final TextField apellidos = new TextField("Apellidos");
    private final EmailField correo = new EmailField("Correo");
    private final PasswordField contrasena = new PasswordField("Contraseña");
    private final TextField telefono = new TextField("Teléfono");
    private final TextField direccion = new TextField("Dirección");

    // --- Password UI mejorado ---
    private final Div pwBar = new Div();
    private final Div seg1 = new Div();
    private final Div seg2 = new Div();
    private final Div seg3 = new Div();
    private final Div seg4 = new Div();
    private final Span ruleLen    = new Span("≥ 8");
    private final Span ruleCase   = new Span("Aa");
    private final Span ruleNumber = new Span("0-9");
    private final Span ruleSymbol = new Span("#!?");
    private final Paragraph pwState = new Paragraph(); // “Fuerza: …”

    private final Binder<Duenno> binder = new Binder<>(Duenno.class);

    public CreacionDuennoView(Duenno_Repository duennoRepository,
                              PasswordEncoder passwordEncoder) {
        this.duennoRepository = duennoRepository;
        this.passwordEncoder = passwordEncoder;

        // Fondo claro consistente
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
                .set("margin", "32px")
                .set("max-width", "900px")
                .set("width", "92%")
                .set("border", "1px solid #f1f1f1");

        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        var badge = VaadinIcon.USER_STAR.create();
        badge.setColor("#ff7a59");
        badge.setSize("36px");

        VerticalLayout titleBox = new VerticalLayout();
        titleBox.setPadding(false);
        titleBox.setSpacing(false);
        H2 title = new H2("Registro de dueño");
        title.getStyle().set("color", "#1f2937").set("margin", "0");
        Paragraph subtitle = new Paragraph("Crea la cuenta de administrador de la empresa.");
        subtitle.getStyle().set("color", "#6b7280").set("margin", "0 0 8px 0");
        titleBox.add(title, subtitle);

        header.add(badge, titleBox);
        header.expand(titleBox);

        // Formulario
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2)
        );

        // Configuración de campos
        nombre.setPlaceholder("Tu nombre");
        nombre.setPrefixComponent(VaadinIcon.USER.create());

        apellidos.setPlaceholder("Tus apellidos");
        apellidos.setPrefixComponent(VaadinIcon.USER.create());

        correo.setPlaceholder("duenno@empresa.com");
        correo.setErrorMessage("Correo no válido");
        correo.setPrefixComponent(VaadinIcon.ENVELOPE.create());
        correo.setWidthFull();

        telefono.setPlaceholder("+34 6XX XX XX XX");
        telefono.setPrefixComponent(VaadinIcon.PHONE.create());
        telefono.setWidthFull();

        direccion.setPlaceholder("Calle y número");
        direccion.setPrefixComponent(VaadinIcon.HOME.create());
        direccion.setWidthFull();

        // Sección de contraseña (a lo ancho)
        Component passwordSection = buildPasswordSection();

        // Añadimos los campos al formulario, en orden lógico y estético
        form.add(
                nombre, apellidos,   // fila 1
                correo,              // fila 2 (ocupa 2 columnas)
                passwordSection,     // fila 3 (ocupa 2 columnas)
                telefono, direccion  // fila 4 (misma línea)
        );

        // Ajustes de anchura de filas
        form.setColspan(correo, 2);
        form.setColspan(passwordSection, 2);
        // OJO: NO poner colspan a 'direccion' para que quede con 'telefono' en la misma fila

        // Estilo visual: más espacio entre filas
        form.getStyle()
                .set("row-gap", "14px")
                .set("margin-top", "12px");

        // Acciones
        Button guardar = new Button("Confirmar", VaadinIcon.CHECK_CIRCLE.create());
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.getStyle()
                .set("background", "linear-gradient(90deg,#ff7a59,#ff3d3d)")
                .set("color", "white")
                .set("border-radius", "12px");
        guardar.addClickShortcut(Key.ENTER);
        guardar.addClickListener(e -> openConfirmDialog());

        Button limpiar = new Button("Limpiar", VaadinIcon.ERASER.create());
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        limpiar.addClickListener(e -> binder.readBean(new Duenno()));

        Button cancelar = new Button("Cancelar", VaadinIcon.CLOSE_CIRCLE.create());
        cancelar.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        cancelar.addClickShortcut(Key.ESCAPE);
        cancelar.addClickListener(e -> getUI().ifPresent(ui -> ui.getPage().getHistory().back()));

        HorizontalLayout actions = new HorizontalLayout(guardar, limpiar, cancelar);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.BETWEEN);

        card.add(header, form, actions);
        return card;
    }

    // ---------- Password UI ----------
    private Component buildPasswordSection() {
        contrasena.setPlaceholder("Mín. 8 caracteres");
        contrasena.setRevealButtonVisible(true);
        contrasena.setPrefixComponent(VaadinIcon.LOCK.create());
        contrasena.setHelperText("Usa letras, números y símbolos.");
        contrasena.setWidthFull(); // ancho completo

        // Barra segmentada
        segStyle(seg1); segStyle(seg2); segStyle(seg3); segStyle(seg4);
        pwBar.getStyle()
                .set("display", "flex")
                .set("gap", "6px")
                .set("margin-top", "6px");
        pwBar.setWidthFull();

        // Estado
        pwState.getStyle().set("margin", "6px 0 0 0").set("fontWeight", "600");

        // Chips de reglas
        styleRuleChip(ruleLen);
        styleRuleChip(ruleCase);
        styleRuleChip(ruleNumber);
        styleRuleChip(ruleSymbol);
        HorizontalLayout rules = new HorizontalLayout(ruleLen, ruleCase, ruleNumber, ruleSymbol);
        rules.setPadding(false);
        rules.setSpacing(true);
        rules.getStyle().set("margin-top", "6px");
        rules.setWidthFull();

        // Bloque contenedor a ancho completo
        VerticalLayout box = new VerticalLayout(contrasena, pwBar, pwState, rules);
        box.setPadding(false);
        box.setSpacing(false);
        box.setWidthFull();

        contrasena.addValueChangeListener(e -> updatePasswordUI(e.getValue()));
        updatePasswordUI(contrasena.getValue());

        return box;
    }

    private void segStyle(Div seg) {
        seg.getStyle()
                .set("height", "10px")
                .set("border-radius", "999px")
                .set("background", "#e5e7eb")
                .set("flex", "1");
    }

    private void styleRuleChip(Span chip) {
        chip.getStyle()
                .set("padding", "2px 8px")
                .set("border-radius", "999px")
                .set("font-size", "12px")
                .set("background", "#f3f4f6")
                .set("color", "#6b7280")
                .set("border", "1px solid #e5e7eb");
    }

    private void updatePasswordUI(String pw) {
        if (pw == null) pw = "";

        boolean okLen    = pw.length() >= 8;
        boolean okCase   = pw.matches(".*[a-z].*") && pw.matches(".*[A-Z].*");
        boolean okNumber = pw.matches(".*\\d.*");
        boolean okSymbol = pw.matches(".*[^A-Za-z0-9].*");

        int score = 0;
        if (okLen) score++;
        if (okCase) score++;
        if (okNumber) score++;
        if (okSymbol) score++;

        // Reset
        setSegment(seg1, false, "#e5e7eb");
        setSegment(seg2, false, "#e5e7eb");
        setSegment(seg3, false, "#e5e7eb");
        setSegment(seg4, false, "#e5e7eb");

        // Colorear
        if (score >= 1) setSegment(seg1, true, "#ef4444"); // rojo
        if (score >= 2) setSegment(seg2, true, "#f59e0b"); // ámbar
        if (score >= 3) setSegment(seg3, true, "#fbbf24"); // amarillo
        if (score >= 4) setSegment(seg4, true, "#10b981"); // verde

        // Estado
        if (score <= 1) {
            pwState.setText("Fuerza: débil");
            pwState.getStyle().set("color", "#ef4444");
        } else if (score == 2 || score == 3) {
            pwState.setText("Fuerza: media");
            pwState.getStyle().set("color", "#f59e0b");
        } else {
            pwState.setText("Fuerza: fuerte");
            pwState.getStyle().set("color", "#10b981");
        }

        // Chips
        setRuleChip(ruleLen, okLen);
        setRuleChip(ruleCase, okCase);
        setRuleChip(ruleNumber, okNumber);
        setRuleChip(ruleSymbol, okSymbol);
    }

    private void setSegment(Div seg, boolean on, String color) {
        seg.getStyle()
                .set("background", on ? color : "#e5e7eb")
                .set("box-shadow", on ? "0 0 0 1px rgba(0,0,0,0.02) inset" : "none");
    }

    private void setRuleChip(Span chip, boolean ok) {
        if (ok) {
            chip.getStyle().set("background", "#ecfdf5").set("color", "#065f46").set("border", "1px solid #a7f3d0");
        } else {
            chip.getStyle().set("background", "#f3f4f6").set("color", "#6b7280").set("border", "1px solid #e5e7eb");
        }
    }
    // ---------- /Password UI ----------

    private void configureBinding() {
        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .bind(Duenno::getNombre, Duenno::setNombre);

        binder.forField(apellidos)
                .asRequired("Los apellidos son obligatorios")
                .bind(Duenno::getApellidos, Duenno::setApellidos);

        binder.forField(correo)
                .asRequired("El correo es obligatorio")
                .withValidator(new EmailValidator("Correo no válido"))
                .bind(Duenno::getCorreo, Duenno::setCorreo);

        // Contraseña cifrada al guardar
        binder.forField(contrasena)
                .asRequired("La contraseña es obligatoria")
                .withValidator(pw -> pw != null && pw.length() >= 8, "Mínimo 8 caracteres")
                .bind(
                        Duenno::getContrasena,
                        (duenno, valorPlano) -> duenno.setContrasena(passwordEncoder.encode(valorPlano))
                );

        Pattern phone = Pattern.compile("^\\+?[0-9 ()-]{7,}$");
        binder.forField(telefono)
                .withValidator(v -> v == null || phone.matcher(v).matches(),
                        "Formato de teléfono no válido")
                .bind(Duenno::getTelefono, Duenno::setTelefono);

        binder.forField(direccion)
                .bind(Duenno::getDireccion, Duenno::setDireccion);

        binder.readBean(new Duenno());
    }

    private void openConfirmDialog() {
        Duenno tmp = new Duenno();
        try {
            binder.writeBean(tmp);
        } catch (ValidationException ex) {
            Notification.show("Revisa los campos del formulario", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (duennoRepository.existsByCorreoIgnoreCase(tmp.getCorreo())) {
            Notification.show("Ya existe un dueño con ese correo", 3500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Confirmar creación");
        dlg.setModal(true);
        dlg.setDraggable(true);

        Paragraph p = new Paragraph(
                "¿Crear al dueño \"" + tmp.getNombre() + " " + tmp.getApellidos()
                        + "\" con correo \"" + tmp.getCorreo() + "\"?"
        );

        Button confirmar = new Button("Confirmar", e -> {
            saveAndRedirect();
            dlg.close();
        });
        confirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelar = new Button("Cancelar", e -> dlg.close());
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dlg.add(p);
        dlg.getFooter().add(cancelar, confirmar);
        dlg.open();
    }

    private void saveAndRedirect() {
        Duenno d = new Duenno();
        try {
            binder.writeBean(d);

            if (duennoRepository.existsByCorreoIgnoreCase(d.getCorreo())) {
                Notification.show("Ya existe un dueño con ese correo", 3500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            duennoRepository.save(d);

            Notification n = new Notification(new Text("Dueño creado correctamente"));
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            n.setDuration(2000);
            n.open();

            // Redirige a la vista de login del dueño
            getUI().ifPresent(ui -> ui.navigate(DuennoLoginView.class));

        } catch (ValidationException ex) {
            Notification.show("Revisa los campos del formulario", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            Notification.show("No se pudo guardar: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
