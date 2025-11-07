package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.services.GestionarDueno;
import es.uca.orderflow.persistence.data.Duenno_Repository;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;

@PageTitle("Editar dueño")
@Route("/backoffice/Duennos/editar/:id")
@AnonymousAllowed
@CssImport("./styles/empleados.css")  // Reutilizamos estilos (form-card, header, fondo, etc.)
public class EditarDuennoView extends VerticalLayout implements BeforeEnterObserver {

    private final Duenno_Repository Duenno_Repository;
    private final GestionarDueno gestionarDueno;

    // UI
    private final VerticalLayout page = new VerticalLayout();
    private final TextField nombre = new TextField("Nombre");
    private final TextField apellidos = new TextField("Apellidos");
    private final EmailField correo = new EmailField("Correo");
    private final TextField telefono = new TextField("Teléfono");
    private final TextField direccion = new TextField("Dirección");

    private final Binder<Duenno> binder = new Binder<>(Duenno.class);

    // Estado
    private Long DuennoId;
    private Duenno DuennoManaged;

    public EditarDuennoView(Duenno_Repository Duenno_Repository, GestionarDueno gestionarDueno) {
        this.Duenno_Repository = Duenno_Repository;
        this.gestionarDueno = gestionarDueno;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // Fondo coherente con el backoffice
        addClassName("backoffice-bg");

        add(buildHeader(), buildCard());
        configureBinding();
    }

    /* -------------------- Header -------------------- */
    private Component buildHeader() {
        // Título + subtítulo con el mismo estilo que Duennos/Empleados
        Div emoji = new Div();
        emoji.addClassName("section-chip"); // punto de color
        H1 title = new H1("Editar dueño");
        Paragraph subtitle = new Paragraph("Modifica los datos del propietario y guarda los cambios.");
        Div titleBox = new Div(title, subtitle);

        HorizontalLayout left = new HorizontalLayout(emoji, titleBox);
        left.setAlignItems(Alignment.CENTER);
        left.setSpacing(true);

        Button volver = new Button("Volver", VaadinIcon.ARROW_LEFT.create());
        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volver.addClassName("btn-ghost");
        volver.addClickListener(e -> UI.getCurrent().navigate("/backoffice/duennos"));

        HorizontalLayout header = new HorizontalLayout(left, volver);
        header.addClassName("empleados-header");
        header.setWidth("min(1200px, 92vw)");
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return header;
    }

    /* -------------------- Card con formulario -------------------- */
    private Component buildCard() {
        Div card = new Div();
        card.addClassName("form-card");
        card.addClassName("form-card--loud");
        card.getStyle().set("padding", "22px");

        H3 blockTitle = new H3("Datos del propietario");
        Hr hr = new Hr();
        hr.addClassName("section-hr");

        // Form grid
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2)
        );

        nombre.setPlaceholder("Nombre del dueño");
        apellidos.setPlaceholder("Apellidos");
        correo.setPlaceholder("ejemplo@dominio.com");
        correo.setErrorMessage("Correo no válido");
        telefono.setPlaceholder("+34 6XX XX XX XX");
        direccion.setPlaceholder("Calle, número, piso…");

        form.add(nombre, apellidos, correo, telefono, direccion);
        form.setColspan(direccion, 2);

        // Acciones
        Button reset = new Button("Restablecer", VaadinIcon.ROTATE_LEFT.create());
        reset.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        reset.addClickListener(e -> recargarDesdeBD());

        Button guardar = new Button("Guardar cambios", VaadinIcon.CHECK_CIRCLE.create());
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.addClassName("btn-save-strong");
        guardar.addClickShortcut(Key.ENTER);
        guardar.addClickListener(e -> guardarCambios());

        HorizontalLayout actions = new HorizontalLayout(reset, guardar);
        actions.addClassName("empleado-actions");
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.END);

        card.add(blockTitle, hr, form, actions);

        page.setWidth("min(1200px, 92vw)");
        page.setPadding(false);
        page.setSpacing(false);
        page.add(card);

        return page;
    }

    /* -------------------- Binder/Validaciones -------------------- */
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

        binder.forField(telefono)
                .bind(Duenno::getTelefono, Duenno::setTelefono);

        binder.forField(direccion)
                .bind(Duenno::getDireccion, Duenno::setDireccion);
    }

    /* -------------------- Navegación: leer :id -------------------- */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> idOpt = event.getRouteParameters().getLong("id");
        if (idOpt.isEmpty()) {
            Notification.show("Falta el id del dueño", 2500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            event.forwardTo("/backoffice/Duennos");
            return;
        }
        this.DuennoId = idOpt.get();
        recargarDesdeBD();
    }

    /* -------------------- Carga desde BD -------------------- */
    private void recargarDesdeBD() {
        //this.DuennoManaged = Duenno_Repository.findById(DuennoId).orElse(null);

        try
        {
            this.DuennoManaged = gestionarDueno.buscarDuennoPorId(DuennoId);

        } catch (Exception e)
        {
            Notification.show("Dueño no encontrado", 2500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("/backoffice/Duennos");
        }
        /*
        //if (DuennoManaged == null) {
            Notification.show("Dueño no encontrado", 2500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            UI.getCurrent().navigate("/backoffice/Duennos");
            return;
        }
        */


        // Rellena el formulario
        binder.readBean(DuennoManaged);
    }

    /* -------------------- Guardar -------------------- */
    private void guardarCambios() {
        try {
            binder.writeBean(DuennoManaged);
         //   DuennoManaged = Duenno_Repository.save(DuennoManaged);
            DuennoManaged = gestionarDueno.modificarDuenno(DuennoManaged);

            Notification n = Notification.show("Cambios guardados", 2000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Opcional: volver al listado
            // UI.getCurrent().navigate("/backoffice/Duennos");

        } catch (ValidationException ex) {
            Notification.show("Revisa los campos del formulario", 2800, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (DataIntegrityViolationException ex) {
            Notification.show("Ese correo ya está registrado", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            Notification.show("No se pudo guardar: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}
