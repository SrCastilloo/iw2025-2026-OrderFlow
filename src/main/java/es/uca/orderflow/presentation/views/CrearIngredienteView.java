package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.validator.RegexpValidator;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import es.uca.orderflow.business.entities.Ingrediente;
import es.uca.orderflow.business.services.DuennoSesionService;
import es.uca.orderflow.business.services.GestionarIngredientes;
import es.uca.orderflow.i18n.SimpleI18NProvider; // 🚨 IMPORTANTE: Añadir este import
import com.vaadin.flow.server.auth.AnonymousAllowed;


@PageTitle("Crear Ingrediente")
@Route("/backoffice/ingredientes/crear")
@AnonymousAllowed
public class CrearIngredienteView extends VerticalLayout implements BeforeEnterObserver {

    private final GestionarIngredientes gestionarIngrediente;
    private final DuennoSesionService duennoSesionService;
    private final SimpleI18NProvider i18nProvider; // 🚨 NUEVO: Referencia al proveedor I18N

    // Campos del formulario
    // 🚨 CAMBIO: Se inicializa con un placeholder temporal para evitar errores
    private final TextField nombre = new TextField("Nombre");

    // Binder para enlazar la entidad con los campos
    private final Binder<Ingrediente> binder = new Binder<>(Ingrediente.class);

    // URL de la imagen de fondo (debería ser una URL directa, no una búsqueda de Google)
    private static final String BACKGROUND_IMAGE_URL = "https://img.freepik.com/foto-gratis/vista-superior-tortilla-mexicana-espacio-copia_23-2148614430.jpg?semt=ais_hybrid&w=740&q=80";

    // 🚨 CAMBIO: Se inyecta SimpleI18NProvider en el constructor
    public CrearIngredienteView(GestionarIngredientes gestionarIngrediente, DuennoSesionService duennoSesionService, SimpleI18NProvider i18nProvider) {
        this.gestionarIngrediente = gestionarIngrediente;
        this.duennoSesionService = duennoSesionService;
        this.i18nProvider = i18nProvider; // 🚨 Se asigna la referencia

        // 🚨 CAMBIO: Se aplica la traducción correcta al campo 'nombre'
        nombre.setLabel(tr("ingredient.name"));


        addClassName("crear-ingrediente-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        // 1. ESTILO VISTOSO: IMAGEN DE FONDO
        getStyle()
                .set("background-image", "url(" + BACKGROUND_IMAGE_URL + ")")
                .set("background-size", "cover")
                .set("background-position", "center center")
                .set("background-attachment", "fixed");

        // Configuración del Binder y Validación de texto
        binder.bindInstanceFields(this);

        binder.forField(nombre)
                // 🚨 Usar el método auxiliar tr()
                .asRequired(tr("validation.required"))
                // 🚨 Usar el método auxiliar tr()
                .withValidator(new RegexpValidator(
                        tr("validation.no_numbers"),
                        "^[\\p{L} .'-]+$"
                ))
                .bind(Ingrediente::getNombre, Ingrediente::setNombre);

        binder.setBean(new Ingrediente());


        // Configuración de los campos
        nombre.setRequiredIndicatorVisible(true);
        // 🚨 Usar el método auxiliar tr() (resuelve el error de "!clave!")
        nombre.setPlaceholder(tr("ingredient.name_placeholder"));
        nombre.setWidth("350px");
        nombre.addThemeVariants(TextFieldVariant.LUMO_ALIGN_CENTER);

        // Inyectar CSS para estilos de campo personalizados y Glassmorphism
        injectFieldCss();

        // Botones
        // 🚨 Usar el método auxiliar tr()
        Button guardar = new Button(tr("button.save_ingredient"), VaadinIcon.CHECK.create(), this::confirmarGuardado);
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        guardar.getStyle().set("padding", "10px 30px");

        // Botón Volver con estilo más suave para Glassmorphism
        // 🚨 Usar el método auxiliar tr()
        Button cancelar = new Button(tr("button.back"), VaadinIcon.ARROW_BACKWARD.create(), e -> getUI().ifPresent(ui -> ui.navigate(DuennoDashboardView.class)));
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelar.getStyle().set("color", "var(--lumo-primary-color)"); // Color primario para el texto

        HorizontalLayout botones = new HorizontalLayout(guardar, cancelar);
        botones.setSpacing(true);
        botones.setPadding(true);

        // Layout del formulario
        FormLayout formLayout = new FormLayout();
        formLayout.add(nombre);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        // Estilo del Contenedor Principal (CARD - GLASSMORPHISM)
        VerticalLayout mainLayout = new VerticalLayout(
                // 🚨 Usar el método auxiliar tr()
                new H3(tr("view.create_ingredient.title") + " 🍅"),
                formLayout,
                botones
        );
        mainLayout.addClassName("ingredient-card");
        mainLayout.setAlignItems(Alignment.CENTER);
        mainLayout.setMaxWidth("450px");
        mainLayout.setWidth("90%");
        mainLayout.setSpacing(true);
        mainLayout.getStyle().set("padding", "20px");

        // 2. ESTILOS GLASSMORPHISM
        mainLayout.getStyle()
                .set("background", "rgba(255, 255, 255, 0.4)") // Fondo semi-transparente blanco
                .set("border-radius", "25px") // Esquinas muy redondeadas
                .set("border", "1px solid rgba(255, 255, 255, 0.2)") // Borde sutil
                .set("box-shadow", "0 8px 32px 0 rgba(31, 38, 135, 0.37)"); // Sombra azulada elegante

        // Estilo del título (texto más visible)
        H3 title = (H3) mainLayout.getComponentAt(0);
        title.getStyle()
                .set("color", "var(--lumo-primary-color)") // Color primario
                .set("font-weight", "800") // Más audaz
                .set("margin-bottom", "10px");

        add(mainLayout);
    }

    /**
     * 🚨 MÉTODO AUXILIAR PARA TRADUCCIONES
     * Llama al I18NProvider inyectado con el Locale actual de la UI.
     */
    private String tr(String key, Object... params) {
        return i18nProvider.getTranslation(key, UI.getCurrent().getLocale(), params);
    }


    // Método auxiliar para inyectar CSS (no cambia)
    private void injectFieldCss() {
        // ... (CSS code remains the same)
        String css = """
            .v-has-label .v-label {
                font-weight: 600; 
            }
            vaadin-text-field {
                --lumo-size-m: 45px; /* Ligeramente más alto */
            }
            .ingredient-card {
                /* CLAVE DEL GLASSMORPHISM */
                backdrop-filter: blur(10px); 
                -webkit-backdrop-filter: blur(10px); 
            }
            .ingredient-card vaadin-text-field [part="input-field"] {
                border-radius: 12px; /* Más redondeado */
                background: rgba(255, 255, 255, 0.8); /* Fondo del input casi blanco para contraste */
                border: 1px solid var(--lumo-contrast-20pct);
                transition: border-color 0.2s, box-shadow 0.2s;
            }
            .ingredient-card vaadin-text-field:hover [part="input-field"] {
                 border-color: var(--lumo-contrast-50pct);
            }
            .ingredient-card vaadin-text-field:focus-within [part="input-field"] {
                border-color: var(--lumo-primary-color);
                box-shadow: 0 0 0 2px var(--lumo-primary-color-50pct);
            }
        """;
        UI.getCurrent().getPage().executeJs(
                "const style = document.createElement('style'); style.textContent = $0; document.head.appendChild(style);",
                css
        );
    }

    // Método de seguridad (no cambia)
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (duennoSesionService.getActual() == null) {
            // ... (forward a la vista de login)
        }
    }

    // CONFIRMACIÓN PREVIA AL GUARDADO
    private void confirmarGuardado(ClickEvent<Button> event) {
        if (!binder.isValid()) {
            // 🚨 Usar el método auxiliar tr()
            Notification n = Notification.show(tr("notification.validation_error"), 3000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Ingrediente ingrediente = binder.getBean();

        ConfirmDialog dialog = new ConfirmDialog();
        // 🚨 Usar el método auxiliar tr()
        dialog.setHeader(tr("dialog.confirm.header"));
        // 🚨 Usar el método auxiliar tr() con parámetro
        dialog.setText(tr("dialog.confirm.text", ingrediente.getNombre()));

        dialog.setCancelable(true);
        // 🚨 Usar el método auxiliar tr()
        dialog.setConfirmText(tr("button.save_ingredient"));
        dialog.setConfirmButtonTheme("primary");

        dialog.addConfirmListener(e -> guardarIngrediente(ingrediente));

        dialog.open();
    }

    /**
     * Lógica de guardado y MANEJO DE ERROR DE DUPLICIDAD
     */
    private void guardarIngrediente(Ingrediente nuevoIngrediente) {
        try {
            gestionarIngrediente.crearIngrediente(nuevoIngrediente);

            // 🚨 Usar el método auxiliar tr() con parámetro
            String successMsg = tr("notification.ingredient_created_success", nuevoIngrediente.getNombre());
            Notification.show(successMsg, 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Limpiar el formulario para un nuevo ingrediente
            binder.setBean(new Ingrediente());

        } catch (Exception e) {
            String errorMessage;

            String lowerCaseMessage = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

            // Detección del error de clave única usando patrones del mensaje SQL/JPA
            if (lowerCaseMessage.contains("duplicate entry") || lowerCaseMessage.contains("uk_ingrediente_nombre")) {

                // 🚨 Usar el método auxiliar tr() con parámetro
                errorMessage = tr("error.duplicate_entry", nuevoIngrediente.getNombre());

            } else {
                // Mensaje genérico para cualquier otro error (conexión, nulos, etc.)
                // 🚨 Usar el método auxiliar tr() con parámetro
                errorMessage = tr("error.unexpected", e.getMessage());
            }

            Notification n = Notification.show(errorMessage, 5000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
}