package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.entities.MenuComposicion;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.ProductoTipo;
import es.uca.orderflow.business.services.DuennoSesionService;
import es.uca.orderflow.business.services.GestionarMenu;
import es.uca.orderflow.business.services.GestionarProducto;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Editar Menú")
@Route("/backoffice/menus/editar/:id")
@AnonymousAllowed
@CssImport("./styles/create-product.css")
public class EditarMenuView extends VerticalLayout implements BeforeEnterObserver {

    private final GestionarMenu gestionarMenu;
    private final GestionarProducto gestionarProducto;
    private final DuennoSesionService duennoSesionService;

    // UI
    private final VerticalLayout composicionList = new VerticalLayout();
    private final Binder<Producto> binder = new Binder<>(Producto.class);

    private final TextField nombre = new TextField("Nombre");
    private final TextField descripcion = new TextField("Descripción");
    private final BigDecimalField precio = new BigDecimalField("Precio");
    private final TextField foto = new TextField("Foto / URL");
    private final Image preview = new Image("", "Vista previa");

    // Estado
    private Long menuIdParam;
    private Producto menuManaged;

    // Cache de productos “normales” para evitar recargar por fila
    private List<Producto> productosDisponibles = new ArrayList<>();

    public EditarMenuView(GestionarMenu gestionarMenu,
                          GestionarProducto gestionarProducto,
                          DuennoSesionService duennoSesionService) {

        this.gestionarMenu = gestionarMenu;
        this.gestionarProducto = gestionarProducto;
        this.duennoSesionService = duennoSesionService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        getStyle().set("background",
                "radial-gradient(1000px 500px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                        "radial-gradient(900px 450px at 110% 8%, rgba(255,120,90,.28), transparent 60%)," +
                        "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)");

        // HERO
        Icon heroIcon = VaadinIcon.COFFEE.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 title = new H1("Editar menú");
        Paragraph subtitle = new Paragraph("Modifica la información del menú y su composición.");
        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.setAlignItems(Alignment.CENTER);
        hero.getStyle().set("margin-top", "6vh").set("margin-bottom", "2vh");

        Div page = new Div();
        page.addClassName("page-wrap");

        Div card = new Div();
        card.addClassName("form-card");
        card.addClassName("form-card--loud");

        H3 blockTitle = new H3("Datos del menú");
        Hr sepTop = new Hr();

        // FORM
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("820px", 2)
        );

        nombre.setPrefixComponent(new Icon(VaadinIcon.TAG));
        nombre.setPlaceholder("Nombre del menú");
        nombre.setClearButtonVisible(true);
        nombre.setWidthFull();

        descripcion.setPrefixComponent(new Icon(VaadinIcon.CLIPBOARD_TEXT));
        descripcion.setPlaceholder("Descripción breve");
        descripcion.setClearButtonVisible(true);
        descripcion.setWidthFull();

        precio.setPrefixComponent(new Icon(VaadinIcon.EURO));
        precio.setPlaceholder("0,00");
        precio.setWidthFull();

        foto.setPrefixComponent(new Icon(VaadinIcon.CAMERA));
        foto.setPlaceholder("https://… o nombre de archivo");
        foto.setClearButtonVisible(true);
        foto.setWidthFull();
        foto.setValueChangeMode(ValueChangeMode.ON_CHANGE);

        preview.setWidth("100%");
        preview.getStyle()
                .set("border-radius", "12px")
                .set("background", "rgba(0,0,0,.04)")
                .set("object-fit", "cover");

        Div previewWrap = new Div(new Paragraph("Vista previa"), preview);
        previewWrap.addClassName("preview-card");

        foto.addValueChangeListener(e -> actualizarPreview(e.getValue()));

        // COMPOSICIÓN
        VerticalLayout compHeader = new VerticalLayout();
        compHeader.setPadding(false);
        compHeader.setSpacing(false);

        HorizontalLayout rowHeader = new HorizontalLayout();
        rowHeader.setWidthFull();
        rowHeader.setAlignItems(Alignment.CENTER);

        H3 compTitle = new H3("Composición del menú");
        Button addRow = new Button("Añadir producto", VaadinIcon.PLUS.create());
        addRow.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addRow.addClickListener(e -> composicionList.add(createComposicionRow(null)));

        rowHeader.add(compTitle);
        rowHeader.expand(compTitle);
        rowHeader.add(addRow);

        Hr sepComp = new Hr();
        compHeader.add(rowHeader, sepComp);

        composicionList.setPadding(false);
        composicionList.setSpacing(true);
        composicionList.addClassName("ing-box");

        nombre.setRequiredIndicatorVisible(true);
        descripcion.setRequiredIndicatorVisible(true);
        precio.setRequiredIndicatorVisible(true);
        foto.setRequiredIndicatorVisible(true);

        nombre.setHelperText("Máx. 60 caracteres");
        descripcion.setHelperText("Una frase clara del menú");
        precio.setHelperText("Dos decimales como máximo");
        foto.setHelperText("URL completa o fichero servido");

        form.add(nombre, descripcion, precio, foto, compHeader, composicionList, previewWrap);
        form.setColspan(nombre, 2);
        form.setColspan(descripcion, 2);
        form.setColspan(foto, 2);
        form.setColspan(compHeader, 2);
        form.setColspan(composicionList, 2);
        form.setColspan(previewWrap, 2);

        // ACCIONES
        Hr sepBottom = new Hr();

        Button cancelar = new Button("Volver", new Icon(VaadinIcon.ARROW_LEFT));
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelar.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/duennopanel")));

        Button reset = new Button("Restablecer", new Icon(VaadinIcon.ROTATE_LEFT));
        reset.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        reset.addClickListener(e -> recargarDesdeBD());

        Button guardar = new Button("Guardar cambios", new Icon(VaadinIcon.CHECK_CIRCLE));
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.addClickShortcut(Key.ENTER);
        guardar.addClickListener(e -> guardarCambios());

        HorizontalLayout actions = new HorizontalLayout(cancelar, reset, guardar);
        actions.addClassName("action-bar");
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        // BINDER
        binder.forField(nombre)
                .asRequired("El nombre es obligatorio")
                .withValidator(new StringLengthValidator("Máximo 60 caracteres", 1, 60))
                .bind(Producto::getNombre, Producto::setNombre);

        binder.forField(descripcion)
                .asRequired("La descripción es obligatoria")
                .withValidator(new StringLengthValidator("Máximo 160 caracteres", 1, 160))
                .bind(Producto::getDescripcion, Producto::setDescripcion);

        binder.forField(precio)
                .asRequired("El precio es obligatorio")
                .withValidator(v -> v == null || v.scale() <= 2, "Máximo 2 decimales")
                .withValidator(v -> v == null || v.compareTo(BigDecimal.ZERO) >= 0, "No puede ser negativo")
                .bind(Producto::getPrecio, Producto::setPrecio);

        binder.forField(foto)
                .asRequired("Incluye una URL o nombre de imagen")
                .withValidator(v -> v != null && v.length() <= 200, "Máximo 200 caracteres")
                .bind(Producto::getFoto, Producto::setFoto);

        card.add(blockTitle, sepTop, form, sepBottom, actions);
        page.add(hero, card);
        add(page);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> optId = event.getRouteParameters().getLong("id");
        if (optId.isEmpty()) {
            Notification.show("Falta el id del menú", 2500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            event.forwardTo("/backoffice/duennopanel");
            return;
        }

        Duenno actual = duennoSesionService.getActual();
        if (actual == null) {
            event.forwardTo(DuennoLoginView.class);
            return;
        }

        this.menuIdParam = optId.get();
        recargarDesdeBD();
    }

    private void recargarDesdeBD() {
        try {
            // Carga productos disponibles una vez
            this.productosDisponibles = gestionarProducto.consultarSoloProductos(); // NO menús

            menuManaged = gestionarMenu.obtenerMenu(menuIdParam);

            binder.readBean(null);
            binder.setBean(menuManaged);

            actualizarPreview(menuManaged.getFoto());

            composicionList.removeAll();
            List<MenuComposicion> actuales = gestionarMenu.composicion(menuManaged.getId());

            if (actuales == null || actuales.isEmpty()) {
                composicionList.add(createComposicionRow(null));
            } else {
                actuales.forEach(mc -> composicionList.add(createComposicionRow(mc)));
            }

        } catch (Exception ex) {
            Notification n = Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            getUI().ifPresent(ui -> ui.navigate("/backoffice/duennopanel"));
        }
    }

    private void guardarCambios() {
        try {
            binder.writeBean(menuManaged);

            Map<Long, Integer> productosConCantidad = leerFilasComposicionComoMap();
            if (productosConCantidad.isEmpty()) {
                Notification n = Notification.show("Añade al menos un producto al menú",
                        2500, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Firma fija de 6 parámetros
            Producto actualizado = gestionarMenu.actualizarMenu(
                    menuManaged.getId(),
                    menuManaged.getNombre(),
                    menuManaged.getDescripcion(),
                    menuManaged.getPrecio(),
                    menuManaged.getFoto(),
                    productosConCantidad
            );

            this.menuManaged = actualizado;
            binder.setBean(menuManaged);
            actualizarPreview(menuManaged.getFoto());

            Notification n = Notification.show("Cambios guardados", 2200, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (ValidationException ex) {
            Notification n = Notification.show("Revisa los campos del formulario",
                    3000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);

        } catch (IllegalArgumentException ex) {
            Notification n = Notification.show(ex.getMessage(), 3200, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);

        } catch (Exception ex) {
            Notification n = Notification.show("Error: " + ex.getMessage(), 3500, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    /* ================= Helpers UI ================= */

    private void actualizarPreview(String valor) {
        String v = valor == null ? "" : valor.trim();
        if (v.isBlank()) {
            preview.setSrc("");
            return;
        }
        if (v.startsWith("http://") || v.startsWith("https://") || v.startsWith("data:image/")) {
            preview.setSrc(v);
            return;
        }
        String ctx = VaadinService.getCurrentRequest() != null
                ? VaadinService.getCurrentRequest().getContextPath()
                : "";
        preview.setSrc(v.startsWith("/") ? (ctx + v) : (ctx + "/" + v));
    }

    /**
     * Fila de composición:
     * - Producto (NO menú)
     * - Cantidad (>=1)
     *
     * FIX LAZY: NO hacemos cb.setValue(existente.getProducto()) porque puede ser proxy.
     * En su lugar, cogemos el ID y buscamos el Producto real en productosDisponibles.
     */
    private Div createComposicionRow(MenuComposicion existente) {
        ComboBox<Producto> cb = new ComboBox<>("Producto");
        cb.setItems(productosDisponibles);
        cb.setItemLabelGenerator(p -> p.getNombre() + " (id=" + p.getId() + ")");
        cb.setPlaceholder("Elige producto");
        cb.setWidthFull();

        IntegerField cantidad = new IntegerField("Cantidad");
        cantidad.setMin(1);
        cantidad.setStepButtonsVisible(true);
        cantidad.setWidthFull();
        cantidad.setPlaceholder("1");

        Button remove = new Button(VaadinIcon.TRASH.create());
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        remove.getElement().getStyle().set("align-self", "end");
        remove.addClickListener(e -> composicionList.remove((Div) remove.getParent().get()));

        if (existente != null) {
            // obtener ID sin inicializar proxy (normalmente Hibernate permite getId() en proxies)
            Long prodId = (existente.getProducto() == null) ? null : existente.getProducto().getId();

            if (prodId != null) {
                Producto real = productosDisponibles.stream()
                        .filter(p -> Objects.equals(p.getId(), prodId))
                        .findFirst()
                        .orElse(null);
                if (real != null) cb.setValue(real);
            }

            cantidad.setValue(existente.getCantidad() == null ? 1 : existente.getCantidad());
        } else {
            cantidad.setValue(1);
        }

        Div row = new Div(cb, cantidad, remove);
        row.addClassName("ing-row");
        return row;
    }

    private Map<Long, Integer> leerFilasComposicionComoMap() {
        List<Div> rows = composicionList.getChildren()
                .filter(c -> c instanceof Div && c.getElement().getClassList().contains("ing-row"))
                .map(c -> (Div) c)
                .collect(Collectors.toList());

        Map<Long, Integer> out = new LinkedHashMap<>();

        for (Div row : rows) {
            @SuppressWarnings("unchecked")
            ComboBox<Producto> cb = (ComboBox<Producto>) row.getComponentAt(0);
            IntegerField qty = (IntegerField) row.getComponentAt(1);

            Producto sel = cb.getValue();
            Integer cantidad = qty.getValue();

            if (sel == null && (cantidad == null || cantidad == 0)) continue;

            if (sel == null) throw new IllegalArgumentException("Hay una fila sin producto.");
            if (sel.getId() == null) throw new IllegalArgumentException("Producto inválido (sin id).");
            if (cantidad == null || cantidad < 1)
                throw new IllegalArgumentException("La cantidad de " + sel.getNombre() + " debe ser ≥ 1.");

            if (sel.getTipo() == ProductoTipo.MENU) {
                throw new IllegalArgumentException("Un menú no puede contener otro menú: " + sel.getNombre());
            }

            if (out.containsKey(sel.getId())) {
                throw new IllegalArgumentException("Producto repetido en el menú: " + sel.getNombre());
            }

            out.put(sel.getId(), cantidad);
        }

        return out;
    }
}
