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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.entities.Empresa;
import es.uca.orderflow.business.services.ModificarEmpresa;
import es.uca.orderflow.persistence.data.EmpresaRepository;
import es.uca.orderflow.business.services.DuennoSesionService;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import java.util.Optional;
import java.util.regex.Pattern;

@PageTitle("Empresa")
@Route("/backoffice/empresa")
@AnonymousAllowed
@CssImport("./styles/empleados.css")
public class EmpresaView extends VerticalLayout implements BeforeEnterObserver {

    private final EmpresaRepository empresaRepository;
    private final DuennoSesionService duennoSesionService;

    ModificarEmpresa me;

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
    private final TextField logo           = new TextField("Logo (URL o ruta)");
    private final Image logoPreview        = new Image("", "Logo");

    private final Binder<Empresa> binder = new Binder<>(Empresa.class);

    public EmpresaView(EmpresaRepository empresaRepository,DuennoSesionService duennoSesionService) {
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
        // si hay dueño, se muestra la vista normal
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
        logo.setPlaceholder("https://… /logo.png o /img/logo.png");

        // Logo live preview
        logo.addValueChangeListener(e -> actualizarLogoPreview(e.getValue()));
        // Bienvenida live
        nombreComercial.addValueChangeListener(e -> actualizarBienvenida(e.getValue(), bienvenida));

        form.add(
                nombreComercial, razonSocial,
                cif, correo,
                telefono, nombreWeb,
                direccion1, direccion2,
                ciudad, provincia,
                codigoPostal, pais,
                logo, previewWrap
        );
        form.setColspan(direccion1, 2);
        form.setColspan(direccion2, 2);
        form.setColspan(previewWrap, 2);

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

        binder.forField(logo)
                .bind(Empresa::getLogo, Empresa::setLogo);
    }

    /* ---------- Carga / Guardado ---------- */
    private void cargarEmpresaUnica() {
        // Asumimos 1 sola empresa. Si no existe, se crea.
        Optional<Empresa> opt = empresaRepository.findAll().stream().findFirst();
        empresaManaged = opt.orElseGet(() -> {
            Empresa e = new Empresa();
            e.setNombreComercial("Mi Empresa");
            return empresaRepository.save(e);
        });

        binder.readBean(empresaManaged);
        actualizarLogoPreview(empresaManaged.getLogo());
        // bienvenida
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
            logoPreview.setSrc(ctx + v);
        } else {
            logoPreview.setSrc(ctx + "/" + v);
        }
    }

    private void actualizarBienvenida(String nombre, Span destino) {
        String texto = "Bienvenido a " + (nombre == null || nombre.isBlank() ? "…" : nombre);
        // Si vino un span, actualízalo; si no, intenta encontrarlo en el preview
        if (destino != null) {
            destino.setText(texto);
        } else {
            // ya se creó en buildCard() (primer hijo del preview es img, segundo el span)
            // pero por si hay cambios de orden, buscamos el primer Span del preview area:
            page.getChildren().forEach(c -> {}); // no-op solo para claridad
        }
    }
}
