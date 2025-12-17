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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.EmailValidator;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.entities.Empresa;
import es.uca.orderflow.business.services.DuennoSesionService;
import es.uca.orderflow.persistence.data.EmpresaRepository;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

@PageTitle("Empresa")
@Route("/backoffice/empresa")
@AnonymousAllowed
@CssImport("./styles/empleados.css")
public class EmpresaView extends VerticalLayout implements BeforeEnterObserver {

    private final EmpresaRepository empresaRepository;
    private final DuennoSesionService duennoSesionService;

    // Estado
    private Empresa empresaManaged;

    // UI
    private final VerticalLayout page = new VerticalLayout();

    private final TextField nombreComercial = new TextField("Nombre comercial");
    private final TextField razonSocial    = new TextField("Razón social");
    private final TextField cif            = new TextField("CIF/NIF");
    private final EmailField correo        = new EmailField("Correo");
    private final TextField telefono       = new TextField("Teléfono");
    private final TextField direccion1     = new TextField("Dirección (línea 1)");
    private final TextField direccion2     = new TextField("Dirección (línea 2)");
    private final TextField ciudad         = new TextField("Ciudad");
    private final TextField provincia      = new TextField("Provincia");
    private final TextField codigoPostal   = new TextField("Código postal");
    private final TextField pais           = new TextField("País");
    private final TextField nombreWeb      = new TextField("Nombre web (dominio / título)");

    // Guardamos en BD la ruta pública del logo subido
    private final TextField logoHidden     = new TextField(); // oculto
    private final Image logoPreview        = new Image("", "Logo");

    // Upload
    private final MemoryBuffer logoBuffer  = new MemoryBuffer();
    private final Upload logoUpload        = new Upload(logoBuffer);

    private final Binder<Empresa> binder = new Binder<>(Empresa.class);

    public EmpresaView(EmpresaRepository empresaRepository, DuennoSesionService duennoSesionService) {
        this.empresaRepository = empresaRepository;
        this.duennoSesionService = duennoSesionService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);
        addClassName("backoffice-bg");

        add(buildHeader(), buildCard());
        configureBinding();
        cargarEmpresaUnica();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Duenno actual = duennoSesionService.getActual();
        if (actual == null) {
            event.forwardTo(DuennoLoginView.class);
        }
    }

    /* ---------- Header ---------- */
    private Component buildHeader() {
        Div chip = new Div();
        chip.addClassName("section-chip");

        H1 title = new H1("Datos de la empresa");
        Paragraph subtitle = new Paragraph("Edita la información corporativa. Los cambios se reflejan en el front-office.");

        Div left = new Div(new HorizontalLayout(chip, new Div(title, subtitle)));
        ((HorizontalLayout) left.getChildren().findFirst().get()).setAlignItems(Alignment.CENTER);
        ((HorizontalLayout) left.getChildren().findFirst().get()).setSpacing(true);

        Button volver = new Button("Volver", VaadinIcon.ARROW_LEFT.create());
        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volver.addClassName("btn-ghost");
        volver.addClickListener(e -> UI.getCurrent().navigate("/backoffice/duennopanel"));

        HorizontalLayout header = new HorizontalLayout(left, volver);
        header.addClassName("empleados-header");
        header.setWidth("min(1200px, 92vw)");
        header.setAlignItems(Alignment.CENTER);
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        return header;
    }

    /* ---------- Card + Form ---------- */
    private Component buildCard() {
        Div card = new Div();
        card.addClassName("form-card");
        card.addClassName("form-card--loud");
        card.getStyle().set("padding", "22px");

        H3 blockTitle = new H3("Información general");
        Hr hr = new Hr();
        hr.addClassName("section-hr");

        // Preview logo + “Bienvenido a …”
        Div previewWrap = new Div();
        previewWrap.getStyle()
                .set("display", "flex")
                .set("align-items", "center")
                .set("gap", "16px")
                .set("padding", "10px 14px")
                .set("border", "1px dashed rgba(15,23,42,.1)")
                .set("border-radius", "12px")
                .set("background", "rgba(15,23,42,.03)")
                .set("margin-bottom", "8px");

        logoPreview.setWidth("64px");
        logoPreview.getStyle().set("border-radius", "12px").set("background", "#fff");
        Span bienvenida = new Span();
        bienvenida.getStyle().set("font-weight", "600");
        bienvenida.setText("Bienvenido a …");
        previewWrap.add(logoPreview, bienvenida);

        // Form
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("820px", 2)
        );

        // Placeholders
        nombreComercial.setPlaceholder("Mi Restaurante S.L.");
        razonSocial.setPlaceholder("Mi Restaurante Sociedad Limitada");
        cif.setPlaceholder("B00000000");
        correo.setPlaceholder("info@mirestaurante.com");
        telefono.setPlaceholder("+34 611 111 111");
        direccion1.setPlaceholder("Calle, número");
        direccion2.setPlaceholder("Portal, piso… (opcional)");
        ciudad.setPlaceholder("Sevilla");
        provincia.setPlaceholder("Sevilla");
        codigoPostal.setPlaceholder("41001");
        pais.setPlaceholder("España");
        nombreWeb.setPlaceholder("mirestaurante.com o el título a mostrar");

        // Bienvenida live
        nombreComercial.addValueChangeListener(e -> actualizarBienvenida(e.getValue(), bienvenida));

        /* ========================= LOGO (UPLOAD + PREVIEW) ========================= */

        logoHidden.setVisible(false);
        logoHidden.setWidthFull();

        logoUpload.setMaxFiles(1);
        logoUpload.setAutoUpload(true);
        logoUpload.setDropAllowed(true);
        logoUpload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        logoUpload.getElement().setProperty("accept", ".jpg,.jpeg,.png,.webp");
        logoUpload.setMaxFileSize(5 * 1024 * 1024);

        Div uploadWrap = new Div();
        uploadWrap.getStyle()
                .set("border-radius", "14px")
                .set("padding", "14px")
                .set("border", "1px dashed rgba(15,23,42,.15)")
                .set("background", "rgba(255,255,255,.55)");

        H4 upTitle = new H4("Logo de la empresa");
        upTitle.getStyle().set("margin", "0 0 6px 0");
        Paragraph upHelp = new Paragraph("Selecciona una imagen (.jpg, .jpeg, .png, .webp). Máx 5MB.");
        upHelp.getStyle().set("margin", "0 0 10px 0").set("opacity", "0.8");

        Button quitarLogo = new Button("Quitar logo", VaadinIcon.CLOSE_SMALL.create());
        quitarLogo.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        quitarLogo.addClickListener(ev -> {
            logoUpload.clearFileList();
            logoHidden.clear();
            logoPreview.setSrc("");
        });

        logoUpload.addSucceededListener(e -> {
            try {
                String fileName = e.getFileName();
                String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

                // Carpeta de guardado (puedes cambiarla)
                Path dir = Paths.get(System.getProperty("user.dir"), "frontend-resources", "company-logos")
                        .toAbsolutePath().normalize();
                if (!Files.exists(dir)) Files.createDirectories(dir);

                String storedName = UUID.randomUUID() + extension;
                Path dest = dir.resolve(storedName);

                try (InputStream in = logoBuffer.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }

                // Ruta pública que va a BD
                String publicPath = "/company-logos/" + storedName;
                logoHidden.setValue(publicPath);

                // Preview
                String ctx = (VaadinService.getCurrentRequest() != null)
                        ? VaadinService.getCurrentRequest().getContextPath()
                        : "";
                logoPreview.setSrc(ctx + publicPath + "?t=" + System.currentTimeMillis());

                Notification.show("Logo guardado", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                ex.printStackTrace();
                Notification.show("Error guardando logo: " + ex.getMessage(), 3500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                logoUpload.clearFileList();
            }
        });

        logoUpload.addFailedListener(e -> {
            Notification.show("Error subiendo el logo", 3500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            logoUpload.clearFileList();
        });

        uploadWrap.add(upTitle, upHelp, logoUpload, quitarLogo);

        /* ========================= FIN LOGO ========================= */

        form.add(
                nombreComercial, razonSocial,
                cif, correo,
                telefono, nombreWeb,
                direccion1, direccion2,
                ciudad, provincia,
                codigoPostal, pais,
                uploadWrap, previewWrap,
                logoHidden
        );
        form.setColspan(direccion1, 2);
        form.setColspan(direccion2, 2);
        form.setColspan(uploadWrap, 2);
        form.setColspan(previewWrap, 2);
        form.setColspan(logoHidden, 2);

        // Acciones
        Button reset = new Button("Restablecer", VaadinIcon.ROTATE_LEFT.create());
        reset.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        reset.addClickListener(e -> cargarEmpresaUnica());

        Button verWeb = new Button("Ver front-office", VaadinIcon.EXTERNAL_LINK.create());
        verWeb.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        verWeb.addClickListener(e -> UI.getCurrent().getPage().open("/", "_blank"));

        Button guardar = new Button("Guardar cambios", VaadinIcon.CHECK_CIRCLE.create());
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.addClassName("btn-save-strong");
        guardar.addClickShortcut(Key.ENTER);
        guardar.addClickListener(e -> guardarCambios());

        HorizontalLayout actions = new HorizontalLayout(reset, verWeb, guardar);
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

    /* ---------- Binder / Validaciones ---------- */
    private void configureBinding() {
        Pattern phone = Pattern.compile("^\\+?[0-9 ()-]{7,}$");
        Pattern cifNif = Pattern.compile("^[A-Za-z0-9-]{8,12}$");
        Pattern cp = Pattern.compile("^[0-9A-Za-z-]{3,10}$");

        binder.forField(nombreComercial)
                .asRequired("El nombre comercial es obligatorio")
                .bind(Empresa::getNombreComercial, Empresa::setNombreComercial);

        binder.forField(razonSocial)
                .bind(Empresa::getRazonSocial, Empresa::setRazonSocial);

        binder.forField(cif)
                .withValidator(v -> v == null || v.isBlank() || cifNif.matcher(v).matches(),
                        "Formato de CIF/NIF no válido")
                .bind(Empresa::getCif, Empresa::setCif);

        binder.forField(correo)
                .withValidator(new EmailValidator("Correo no válido"))
                .bind(Empresa::getCorreo, Empresa::setCorreo);

        binder.forField(telefono)
                .withValidator(v -> v == null || v.isBlank() || phone.matcher(v).matches(),
                        "Formato de teléfono no válido")
                .bind(Empresa::getTelefono, Empresa::setTelefono);

        binder.forField(direccion1)
                .bind(Empresa::getDireccion1, Empresa::setDireccion1);
        binder.forField(direccion2)
                .bind(Empresa::getDireccion2, Empresa::setDireccion2);
        binder.forField(ciudad)
                .bind(Empresa::getCiudad, Empresa::setCiudad);
        binder.forField(provincia)
                .bind(Empresa::getProvincia, Empresa::setProvincia);

        binder.forField(codigoPostal)
                .withValidator(v -> v == null || v.isBlank() || cp.matcher(v).matches(),
                        "Código postal no válido")
                .bind(Empresa::getCodigoPostal, Empresa::setCodigoPostal);

        binder.forField(pais)
                .bind(Empresa::getPais, Empresa::setPais);

        binder.forField(nombreWeb)
                .bind(Empresa::getNombreWeb, Empresa::setNombreWeb);

        // LOGO: ahora se guarda aquí
        binder.forField(logoHidden)
                .bind(Empresa::getLogo, Empresa::setLogo);
    }

    /* ---------- Carga / Guardado ---------- */
    private void cargarEmpresaUnica() {
        Optional<Empresa> opt = empresaRepository.findAll().stream().findFirst();
        empresaManaged = opt.orElseGet(() -> {
            Empresa e = new Empresa();
            e.setNombreComercial("Mi Empresa");
            return empresaRepository.save(e);
        });

        binder.readBean(empresaManaged);
        actualizarLogoPreview(empresaManaged.getLogo());
        actualizarBienvenida(empresaManaged.getNombreComercial(), null);
    }

    private void guardarCambios() {
        try {
            binder.writeBean(empresaManaged);
            empresaManaged = empresaRepository.save(empresaManaged);

            Notification n = Notification.show("Cambios guardados", 2200, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (ValidationException ex) {
            Notification.show("Revisa los campos del formulario", 2800, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } catch (Exception ex) {
            Notification.show("No se pudo guardar: " + ex.getMessage(), 3800, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /* ---------- Helpers ---------- */
    private void actualizarLogoPreview(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.isBlank()) {
            logoPreview.setSrc("");
            return;
        }
        if (v.startsWith("http://") || v.startsWith("https://") || v.startsWith("data:image/")) {
            logoPreview.setSrc(v);
            return;
        }
        String ctx = VaadinService.getCurrentRequest() != null
                ? VaadinService.getCurrentRequest().getContextPath()
                : "";
        if (v.startsWith("/")) {
            logoPreview.setSrc(ctx + v + (v.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis());
        } else {
            logoPreview.setSrc(ctx + "/" + v + (v.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis());
        }
    }

    private void actualizarBienvenida(String nombre, Span destino) {
        String texto = "Bienvenido a " + (nombre == null || nombre.isBlank() ? "…" : nombre);
        if (destino != null) destino.setText(texto);
    }
}
