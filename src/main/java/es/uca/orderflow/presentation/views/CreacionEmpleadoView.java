package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import es.uca.orderflow.business.entities.Empleado;
import es.uca.orderflow.business.entities.Tipo_Empleado;
import es.uca.orderflow.persistence.data.EmpleadoRepository;
import es.uca.orderflow.persistence.data.Tipo_EmpleadoRepository;

import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.regex.Pattern;

@PageTitle("Alta de empleado")
@Route("/backoffice/registroempleado")
@AnonymousAllowed
public class CreacionEmpleadoView extends VerticalLayout {

    private final EmpleadoRepository empleadoRepository;
    private final Tipo_EmpleadoRepository tipoEmpleadoRepository;
    private final PasswordEncoder passwordEncoder;

    // Campos
    private final TextField nombre = new TextField("Nombre");
    private final TextField apellidos = new TextField("Apellidos");
    private final EmailField correo = new EmailField("Correo");
    private final PasswordField contrasena = new PasswordField("Contraseña");
    private final TextField telefono = new TextField("Teléfono");
    private final TextField direccion = new TextField("Dirección");
    private final ComboBox<Tipo_Empleado> tipo = new ComboBox<>("Tipo de empleado");

    // Preview
    private final Avatar avatarPreview = new Avatar();
    private final Paragraph rolPreview = new Paragraph();

    private final Binder<Empleado> binder = new Binder<>(Empleado.class);

    public CreacionEmpleadoView(EmpleadoRepository empleadoRepository,
                                Tipo_EmpleadoRepository tipoEmpleadoRepository, PasswordEncoder passwordEncoder) {
        this.empleadoRepository = empleadoRepository;
        this.tipoEmpleadoRepository = tipoEmpleadoRepository;
        this.passwordEncoder = passwordEncoder;

        // Fondo claro tipo “peach”
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        getStyle().set("background", "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)");

        add(buildCard());
        configureBinding();
        loadTipos();
        wirePreview();
    }

    private Div buildCard() {
        // Tarjeta blanca con sombra suave
        Div card = new Div();
        card.getStyle()
                .set("background", "#ffffff")
                .set("border-radius", "24px")
                .set("box-shadow", "0 20px 60px rgba(0,0,0,0.10)")
                .set("padding", "28px")
                .set("margin", "32px")
                .set("max-width", "1060px")
                .set("width", "92%")
                .set("border", "1px solid #f1f1f1");

        // Header
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon badge = VaadinIcon.USER_CARD.create();
        badge.setColor("#ff7a59"); // naranja coral
        badge.setSize("36px");

        VerticalLayout titleBox = new VerticalLayout();
        titleBox.setPadding(false);
        titleBox.setSpacing(false);
        H2 title = new H2("Alta de empleado");
        title.getStyle().set("color", "#1f2937").set("margin", "0");
        Paragraph subtitle = new Paragraph("Crea una cuenta de empleado y asigna su rol.");
        subtitle.getStyle().set("color", "#6b7280").set("margin", "0 0 8px 0");

        titleBox.add(title, subtitle);
        header.add(badge, titleBox);
        header.expand(titleBox);

        // Formulario
        FormLayout form = new FormLayout();
        form.getStyle().set("color", "#111827");
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2),
                new FormLayout.ResponsiveStep("1024px", 3)
        );

        // Placeholders + iconos en los campos (coherente con tu registro de clientes)
        nombre.setPlaceholder("Tu nombre");
        nombre.setPrefixComponent(VaadinIcon.USER.create());
        apellidos.setPlaceholder("Tus apellidos");
        apellidos.setPrefixComponent(VaadinIcon.USER.create());

        correo.setPlaceholder("ejemplo@gmail.com");
        correo.setErrorMessage("Correo no válido");
        correo.setPrefixComponent(VaadinIcon.ENVELOPE.create());

        contrasena.setPlaceholder("Mín. 6 caracteres");
        contrasena.setRevealButtonVisible(true);
        contrasena.setPrefixComponent(VaadinIcon.LOCK.create());

        telefono.setPlaceholder("+34 6XX XX XX XX");
        telefono.setPrefixComponent(VaadinIcon.PHONE.create());

        direccion.setPlaceholder("Calle y número");
        direccion.setPrefixComponent(VaadinIcon.HOME.create());

        tipo.setItemLabelGenerator(Tipo_Empleado::getNombre);
        tipo.setPlaceholder("Selecciona un rol…");
        tipo.setClearButtonVisible(true);
        tipo.setPrefixComponent(VaadinIcon.GROUP.create());

        // Ayudas
        contrasena.setHelperText("Usa letras y números. Mínimo 6 caracteres.");
        correo.setHelperText("Se enviarán credenciales a este correo.");
        telefono.setHelperText("Opcional");

        form.add(nombre, apellidos, tipo, correo, contrasena, telefono, direccion);
        form.setColspan(direccion, 3);

        // Preview (avatar + rol)
        Div preview = buildPreview();

        // Acciones
        Button guardar = new Button("Registrarse", VaadinIcon.CHECK_CIRCLE.create());
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.getStyle()
                .set("background", "linear-gradient(90deg,#ff7a59,#ff3d3d)")
                .set("color", "white")
                .set("border-radius", "12px");
        guardar.addClickShortcut(Key.ENTER);

        Button limpiar = new Button("Limpiar", VaadinIcon.ERASER.create());
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button cancelar = new Button("Cancelar", VaadinIcon.CLOSE_CIRCLE.create());
        cancelar.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        cancelar.addClickShortcut(Key.ESCAPE);

        guardar.addClickListener(e -> openConfirmDialog());
        limpiar.addClickListener(e -> {
            binder.readBean(new Empleado());
            tipo.clear();
            updatePreview(null, null);
        });
        cancelar.addClickListener(e -> getUI().ifPresent(ui -> ui.getPage().getHistory().back()));

        HorizontalLayout actions = new HorizontalLayout(guardar, limpiar, cancelar);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.BETWEEN);
        actions.getStyle().set("margin-top", "8px");

        card.add(header, form, preview, actions);
        return card;
    }

    private Div buildPreview() {
        Div preview = new Div();
        preview.getStyle()
                .set("margin-top", "12px")
                .set("padding", "14px 16px")
                .set("border-radius", "16px")
                .set("border", "1px dashed #e5e7eb")
                .set("background", "#fafafa")
                .set("color", "#374151");

        avatarPreview.setName("Nuevo empleado");
        avatarPreview.setAbbreviation("NE");
        avatarPreview.getStyle().set("margin-right", "12px");

        rolPreview.getStyle()
                .set("margin", "0")
                .set("font-weight", "600")
                .set("color", "#374151");

        HorizontalLayout row = new HorizontalLayout(avatarPreview, rolPreview);
        row.setAlignItems(Alignment.CENTER);

        Paragraph hint = new Paragraph("Previsualización del nombre y el rol seleccionado.");
        hint.getStyle().set("margin", "8px 0 0 0").set("color", "#6b7280");

        preview.add(row, hint);
        return preview;
    }

    private void wirePreview() {
        nombre.addValueChangeListener(e -> updatePreview(nombre.getValue(), getRol()));
        apellidos.addValueChangeListener(e -> updatePreview(nombre.getValue(), getRol()));
        tipo.addValueChangeListener(e -> updatePreview(nombre.getValue(), getRol()));
        updatePreview(null, null);
    }

    private String getRol() {
        return tipo.getValue() != null ? tipo.getValue().getNombre() : null;
    }

    private void updatePreview(String nombreEmpleado, String rol) {
        String display = (nombreEmpleado == null || nombreEmpleado.isBlank())
                ? "Nuevo empleado" : nombreEmpleado;
        avatarPreview.setName(display);
        avatarPreview.setAbbreviation(display.length() >= 2
                ? display.substring(0, 2).toUpperCase()
                : "NE");
        rolPreview.setText(rol == null ? "Sin rol seleccionado" : "Rol: " + rol);
    }

    private void configureBinding() {
        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .bind(Empleado::getNombre, Empleado::setNombre);

        binder.forField(apellidos)
                .asRequired("Los apellidos son obligatorios")
                .bind(Empleado::getApellidos, Empleado::setApellidos);

        binder.forField(correo)
                .asRequired("El correo es obligatorio")
                .withValidator(new EmailValidator("Correo no válido"))
                .bind(Empleado::getCorreo, Empleado::setCorreo);

        binder.forField(contrasena)
                .asRequired("La contraseña es obligatoria")
                .withValidator(pw -> pw != null && pw.length() >= 6, "Mínimo 6 caracteres")
                .bind(
                        Empleado::getContrasena,
                        (empleado, valorPlano) -> empleado.setContrasena(passwordEncoder.encode(valorPlano))
                );
        Pattern phone = Pattern.compile("^\\+?[0-9 ()-]{7,}$");
        binder.forField(telefono)
                .withValidator(v -> v == null || phone.matcher(v).matches(),
                        "Formato de teléfono no válido")
                .bind(Empleado::getTelefono, Empleado::setTelefono);

        binder.forField(direccion)
                .bind(Empleado::getDireccion, Empleado::setDireccion);

        // OJO: tu entidad usa getTipoEmpleado/setTipoEmpleado
        binder.forField(tipo)
                .asRequired("Selecciona un tipo")
                .bind(Empleado::getTipoEmpleado, Empleado::setTipoEmpleado);
    }

    private void loadTipos() {
        List<Tipo_Empleado> tipos = tipoEmpleadoRepository.findAll(Sort.by("nombre").ascending());
        tipo.setItems(tipos);
    }

    // --- Confirmación + Guardado ---

    private void openConfirmDialog() {
        Empleado e = new Empleado();
        try {
            binder.writeBean(e);
        } catch (ValidationException ex) {
            Notification.show("Revisa los campos del formulario", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Confirmar creación");
        dlg.setModal(true);
        dlg.setDraggable(true);

        Paragraph p = new Paragraph(
                "¿Crear el empleado \"" + e.getNombre() + " " + e.getApellidos()
                        + "\" con rol \"" + (e.getTipoEmpleado() != null ? e.getTipoEmpleado().getNombre() : "—")
                        + "\" y correo \"" + e.getCorreo() + "\"?"
        );

        Button confirmar = new Button("Confirmar", ev -> {
            saveEmpleado();
            dlg.close();
        });
        confirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelar = new Button("Cancelar", ev -> dlg.close());
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dlg.add(p);
        dlg.getFooter().add(cancelar, confirmar);
        dlg.open();
    }

    private void saveEmpleado() {
        Empleado e = new Empleado();
        try {
            binder.writeBean(e);
            empleadoRepository.save(e);

            Notification n = new Notification();
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            n.setDuration(3000);
            n.add(new Text("Empleado creado correctamente"));
            n.open();

            binder.readBean(new Empleado());
            tipo.clear();
            updatePreview(null, null);
            getUI().ifPresent(ui -> ui.navigate(EmpleadoLoginView.class));


        } catch (ValidationException ex) {
            Notification.show("Revisa los campos del formulario", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            Notification.show("No se pudo guardar: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
