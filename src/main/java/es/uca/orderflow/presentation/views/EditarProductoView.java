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
import es.uca.orderflow.business.entities.Ingrediente;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.business.services.GestionarIngredientes;
import es.uca.orderflow.business.services.GestionarProducto;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import es.uca.orderflow.business.services.DuennoSesionService;



@PageTitle("Editar Producto")
@Route("/backoffice/productos/editar/:id")
@AnonymousAllowed
@CssImport("./styles/create-product.css")
public class EditarProductoView extends VerticalLayout implements BeforeEnterObserver {


    private final GestionarProducto gestionarProducto;
    private final GestionarIngredientes gestionarIngredientes;
    private final DuennoSesionService duennoSesionService;

    // UI comunes
    private final VerticalLayout ingredientesList = new VerticalLayout();
    private final Binder<Producto> binder = new Binder<>(Producto.class);

    private final TextField nombre = new TextField("Nombre");
    private final TextField descripcion = new TextField("Descripción");
    private final IntegerField stock = new IntegerField("Stock");
    private final BigDecimalField precio = new BigDecimalField("Precio");
    private final TextField foto = new TextField("Foto / URL");
    private final Image preview = new Image("", "Vista previa");

    // Estado
    private Long productoIdParam;
    private Producto productoManaged; // bean en edición

    public EditarProductoView(GestionarProducto gestionarProducto,
                              GestionarIngredientes gestionarIngredientes,DuennoSesionService duennoSesionService) {
        this.gestionarProducto = gestionarProducto;
        this.gestionarIngredientes = gestionarIngredientes;
        this.duennoSesionService = duennoSesionService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // fondo suave como en crear
        getStyle().set("background",
                "radial-gradient(1000px 500px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                        "radial-gradient(900px 450px at 110% 8%, rgba(255,120,90,.28), transparent 60%)," +
                        "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)");

        // HERO
        Icon heroIcon = VaadinIcon.EDIT.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");
        H1 title = new H1("Editar producto");
        Paragraph subtitle = new Paragraph("Modifica la información y guarda los cambios.");
        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.setAlignItems(Alignment.CENTER);
        hero.getStyle().set("margin-top", "6vh").set("margin-bottom", "2vh");

        // PAGE WRAP
        Div page = new Div();
        page.addClassName("page-wrap");

        // CARD
        Div card = new Div();
        card.addClassName("form-card");
        card.addClassName("form-card--loud");

        H3 blockTitle = new H3("Datos del producto");
        Hr sepTop = new Hr();

        // FORM
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("820px", 2)
        );

        nombre.setPrefixComponent(new Icon(VaadinIcon.TAG));
        nombre.setPlaceholder("Nombre del producto");
        nombre.setClearButtonVisible(true);
        nombre.setWidthFull();

        descripcion.setPrefixComponent(new Icon(VaadinIcon.CLIPBOARD_TEXT));
        descripcion.setPlaceholder("Descripción breve");
        descripcion.setClearButtonVisible(true);
        descripcion.setWidthFull();

        stock.setPrefixComponent(new Icon(VaadinIcon.CUBE));
        stock.setSuffixComponent(new Icon(VaadinIcon.EXCHANGE));
        stock.setStep(1);
        stock.setMin(0);
        stock.setPlaceholder("Unidades disponibles");
        stock.setWidthFull();

        precio.setPrefixComponent(new Icon(VaadinIcon.EURO));
        precio.setPlaceholder("0,00");
        precio.setWidthFull();

        foto.setPrefixComponent(new Icon(VaadinIcon.CAMERA));
        foto.setPlaceholder("https://… o nombre de archivo");
        foto.setClearButtonVisible(true);
        foto.setWidthFull();
        foto.setValueChangeMode(ValueChangeMode.ON_CHANGE);

        // preview
        preview.setWidth("100%");
        preview.getStyle()
                .set("border-radius", "12px")
                .set("background", "rgba(0,0,0,.04)")
                .set("object-fit", "cover");
        Div previewWrap = new Div(new Paragraph("Vista previa"), preview);
        previewWrap.addClassName("preview-card");

        foto.addValueChangeListener(e -> actualizarPreview(e.getValue()));

        // Ingredientes
        VerticalLayout ingHeader = new VerticalLayout();
        ingHeader.setPadding(false);
        ingHeader.setSpacing(false);

        HorizontalLayout rowHeader = new HorizontalLayout();
        rowHeader.setWidthFull();
        rowHeader.setAlignItems(Alignment.CENTER);

        H3 ingTitle = new H3("Ingredientes del producto");
        Button addIng = new Button("Añadir ingrediente", VaadinIcon.PLUS.create());
        addIng.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addIng.addClickListener(e -> ingredientesList.add(createIngredientRow(null)));

        rowHeader.add(ingTitle);
        rowHeader.expand(ingTitle);
        rowHeader.add(addIng);

        Hr sepIng = new Hr();
        ingHeader.add(rowHeader, sepIng);

        ingredientesList.setPadding(false);
        ingredientesList.setSpacing(true);
        ingredientesList.addClassName("ing-box");

        // Reglas helper
        nombre.setRequiredIndicatorVisible(true);
        descripcion.setRequiredIndicatorVisible(true);
        stock.setRequiredIndicatorVisible(true);
        precio.setRequiredIndicatorVisible(true);
        foto.setRequiredIndicatorVisible(true);

        nombre.setHelperText("Máx. 60 caracteres");
        descripcion.setHelperText("Una frase clara del producto");
        stock.setHelperText("≥ 0");
        precio.setHelperText("Dos decimales como máximo");
        foto.setHelperText("URL completa o fichero servido");

        // Distribución
        form.add(nombre, descripcion, stock, precio, foto, ingHeader, ingredientesList, previewWrap);
        form.setColspan(nombre, 2);
        form.setColspan(descripcion, 2);
        form.setColspan(foto, 2);
        form.setColspan(ingHeader, 2);
        form.setColspan(ingredientesList, 2);
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

        binder.forField(stock)
                .asRequired("Indica el stock")
                .withValidator(v -> v != null && v >= 0, "Debe ser 0 o más")
                .bind(Producto::getStock, Producto::setStock);

        binder.forField(precio)
                .asRequired("El precio es obligatorio")
                .withValidator(v -> v == null || v.scale() <= 2, "Máximo 2 decimales")
                .withValidator(v -> v == null || v.compareTo(BigDecimal.ZERO) >= 0, "No puede ser negativo")
                .bind(Producto::getPrecio, Producto::setPrecio);

        binder.forField(foto)
                .asRequired("Incluye una URL o nombre de imagen")
                .withValidator(v -> v != null && v.length() <= 200, "Máximo 200 caracteres")
                .bind(Producto::getFoto, Producto::setFoto);

        // montaje
        card.add(blockTitle, sepTop, form, sepBottom, actions);
        page.add(hero, card);
        add(page);
    }

    /* ============ Navegación: obtener :id de la URL y cargar ============ */
    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Optional<Long> optId = event.getRouteParameters().getLong("id");
        if (optId.isEmpty()) {
            Notification.show("Falta el id del producto", 2500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            event.forwardTo("/backoffice/duennopanel");
            return;
        }
        Duenno actual = duennoSesionService.getActual();
        if (actual == null) {
            event.forwardTo(DuennoLoginView.class);
        }
        // si hay dueño, se muestra la vista normal
        this.productoIdParam = optId.get();
        recargarDesdeBD();
    }

    /* ============ Carga/recarga desde BD ============ */
    private void recargarDesdeBD() {
        // carga producto
        //this.productoManaged = productoRepository.findById(productoIdParam)
              //  .orElse(null);

        this.productoManaged = gestionarProducto.buscarProductoPorId(productoIdParam);

        if (productoManaged == null) {
            Notification n = Notification.show("Producto no encontrado", 2500, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            getUI().ifPresent(ui -> ui.navigate("/backoffice/duennopanel"));
            return;
        }

        // Set bean en binder
        binder.readBean(null); // limpia posibles valores previos
        binder.setBean(productoManaged);

        // preview inicial
        actualizarPreview(productoManaged.getFoto());

        // cargar ingredientes actuales
        ingredientesList.removeAll();

        List<Producto_Ingrediente> actuales =
                //productoIngredienteRepository.findByProductoIdWithIngrediente(productoManaged.getId());
                gestionarProducto.encontrarIngredientesPorProductoId(productoManaged.getId());

        if (actuales == null || actuales.isEmpty()) {
            ingredientesList.add(createIngredientRow(null));
        } else {
            actuales.forEach(pi -> ingredientesList.add(createIngredientRow(pi)));
        }


    }


    private void guardarCambios() {
        try {
            // vuelca UI -> bean
            binder.writeBean(productoManaged);

            List<Producto_Ingrediente> nuevas = leerFilasIngredientesManaged(productoManaged);
            if (nuevas.isEmpty()) {
                Notification n = Notification.show("Añade al menos un ingrediente",
                        2500, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            productoManaged = gestionarProducto.actualizarProducto(productoManaged, nuevas);

            Notification n = Notification.show("Cambios guardados", 2200, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

        } catch (ValidationException ex) {
            Notification n = Notification.show("Revisa los campos del formulario",
                    3000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);

        } catch (IllegalArgumentException ex) {
            Notification n = Notification.show(ex.getMessage(), 3000, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);

        } catch (IllegalStateException ex) {
            // ⬅️ conflicto de concurrencia (optimistic locking)
            Notification n = Notification.show(ex.getMessage(), 3500, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }



    /* ============ Helpers UI ============ */

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
        if (v.startsWith("/")) {
            preview.setSrc(ctx + v);
        } else {
            preview.setSrc(ctx + "/" + v);
        }
    }

    /** Crea una fila de ingrediente. Si 'existente' != null, precarga valores. */
    private Div createIngredientRow(Producto_Ingrediente existente) {
        List<Ingrediente> all = gestionarIngredientes.obtenerIngredientes();
        List<String> unidades = Arrays.asList("g", "kg", "ml", "l", "u");

        ComboBox<Ingrediente> cb = new ComboBox<>("Ingrediente");
        cb.setItems(all);
        cb.setItemLabelGenerator(Ingrediente::getNombre);
        cb.setPlaceholder("Elige ingrediente");
        cb.setWidthFull();

        BigDecimalField qty = new BigDecimalField("Cantidad");
        qty.setPlaceholder("0");
        qty.setHelperText("≥ 0");
        qty.setWidthFull();
        qty.getElement().getThemeList().add("helper-above-field");

        ComboBox<String> unit = new ComboBox<>("Unidad");
        unit.setItems(unidades);
        unit.setPlaceholder("g/ml/…");
        unit.setWidthFull();

        Button remove = new Button(VaadinIcon.TRASH.create());
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        remove.getElement().getStyle().set("align-self", "end");
        remove.addClickListener(e -> ingredientesList.remove((Div) remove.getParent().get()));

        // Precarga si tenemos relación existente
        if (existente != null) {
            // Selecciona el ingrediente en el combo (para UI vale el objeto)
            // No usaremos este objeto al guardar; luego reobtenemos la ref managed por id.
            cb.setValue(existente.getIngrediente());
            qty.setValue(existente.getCantidad());
            unit.setValue(existente.getUnidad());
        }

        Div row = new Div(cb, qty, unit, remove);
        row.addClassName("ing-row");
        return row;
    }

    /** Lee filas y devuelve relaciones nuevas usando entidades MANAGED de ingrediente. */
    private List<Producto_Ingrediente> leerFilasIngredientesManaged(Producto productoManaged) {
        List<Div> rows = ingredientesList.getChildren()
                .filter(c -> c instanceof Div && c.getElement().getClassList().contains("ing-row"))
                .map(c -> (Div) c)
                .collect(Collectors.toList());

        List<Producto_Ingrediente> out = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();

        for (Div row : rows) {
            @SuppressWarnings("unchecked")
            ComboBox<Ingrediente> cb = (ComboBox<Ingrediente>) row.getComponentAt(0);
            BigDecimalField qty = (BigDecimalField) row.getComponentAt(1);
            @SuppressWarnings("unchecked")
            ComboBox<String> unit = (ComboBox<String>) row.getComponentAt(2);

            Ingrediente sel = cb.getValue();
            BigDecimal cantidad = qty.getValue();
            String unidad = unit.getValue();

            // fila vacía -> ignorar
            if (sel == null && (cantidad == null || BigDecimal.ZERO.compareTo(cantidad) == 0)
                    && (unidad == null || unidad.isBlank())) continue;

            if (sel == null) throw new IllegalArgumentException("Hay una fila sin ingrediente.");
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("La cantidad de " + sel.getNombre() + " debe ser ≥ 0.");
            if (unidad == null || unidad.isBlank())
                throw new IllegalArgumentException("La unidad de " + sel.getNombre() + " es obligatoria.");
            if (unidad.length() > 8)
                throw new IllegalArgumentException("La unidad para " + sel.getNombre() + " supera 8 caracteres.");
            if (!vistos.add(sel.getId()))
                throw new IllegalArgumentException("Ingrediente repetido: " + sel.getNombre());

            // Referencia MANAGED por id (evita detached/uninitialized proxy)
            Ingrediente ingredienteManaged =
                   gestionarIngredientes.obtenerIngredientePorId(sel.getId());

            Producto_Ingrediente pi = new Producto_Ingrediente();
            pi.setProducto(productoManaged);
            pi.setIngrediente(ingredienteManaged);
            pi.setCantidad(cantidad);
            pi.setUnidad(unidad);

            out.add(pi);
        }
        return out;
    }
}
