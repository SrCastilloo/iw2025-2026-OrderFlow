package es.uca.orderflow.presentation.views;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Ingrediente;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.persistence.data.IngredienteRepository;
import es.uca.orderflow.persistence.data.ProductoRepository;
import es.uca.orderflow.persistence.data.Producto_IngredienteRepository;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Crear Producto")
@Route("/backoffice/productos/crear")
@AnonymousAllowed
@CssImport("./styles/create-product.css")
public class CreaProductoView extends VerticalLayout {

    private final ProductoRepository productoRepository;
    private final IngredienteRepository ingredienteRepository;
    private final Producto_IngredienteRepository productoIngredienteRepository;

    // contenedor de filas dinámicas
    private final VerticalLayout ingredientesList = new VerticalLayout();

    public CreaProductoView(ProductoRepository productoRepository,
                            IngredienteRepository ingredienteRepository,
                            Producto_IngredienteRepository productoIngredienteRepository) {
        this.productoRepository = productoRepository;
        this.ingredienteRepository = ingredienteRepository;
        this.productoIngredienteRepository = productoIngredienteRepository;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // fondo suave
        getStyle().set("background",
                "radial-gradient(1000px 500px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                        "radial-gradient(900px 450px at 110% 8%, rgba(255,120,90,.28), transparent 60%)," +
                        "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)");


        /* ================== HERO ================== */
        Icon heroIcon = VaadinIcon.CUTLERY.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 title = new H1("Crear un nuevo producto");
        title.getStyle().set("margin", "0").set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph("Completa los datos y confirma para publicarlo en el catálogo.");
        subtitle.getStyle().set("margin", "6px 0 0 0").set("opacity", "0.85");

        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.setAlignItems(Alignment.CENTER);
        hero.setSpacing(true);
        hero.setPadding(true);
        hero.getStyle().set("margin-top", "6vh").set("margin-bottom", "2vh");

        /* ======== CONTENEDOR CENTRADO ======== */
        Div page = new Div();
        page.addClassName("page-wrap");
        add(page);

        /* ================== CARD ================== */
        Div card = new Div();
        card.addClassName("form-card");
        card.addClassName("form-card--loud");
        // Cabecera de la card
        H3 blockTitle = new H3("Datos básicos");
        blockTitle.getStyle().set("margin", "0 0 10px 0");

        Hr sepTop = new Hr(); sepTop.getStyle().set("margin", "10px 0 18px 0");

        // ======== FORM ========
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("820px", 2)
        );

        TextField nombre = new TextField("Nombre");
        nombre.setPrefixComponent(new Icon(VaadinIcon.TAG));
        nombre.setPlaceholder("Nombre del producto");
        nombre.setClearButtonVisible(true);
        nombre.setWidthFull();

        TextField descripcion = new TextField("Descripción");
        descripcion.setPrefixComponent(new Icon(VaadinIcon.CLIPBOARD_TEXT));
        descripcion.setPlaceholder("Descripción breve");
        descripcion.setClearButtonVisible(true);
        descripcion.setWidthFull();

        IntegerField stock = new IntegerField("Stock");
        stock.setPrefixComponent(new Icon(VaadinIcon.CUBE));
        stock.setSuffixComponent(new Icon(VaadinIcon.EXCHANGE));
        stock.setStep(1);
        stock.setMin(0);
        stock.setPlaceholder("Unidades disponibles");
        stock.setWidthFull();

        BigDecimalField precio = new BigDecimalField("Precio");
        precio.setPrefixComponent(new Icon(VaadinIcon.EURO));
        precio.setPlaceholder("0,00");
        precio.setWidthFull();

        TextField foto = new TextField("Foto / URL");
        foto.setPrefixComponent(new Icon(VaadinIcon.CAMERA));
        foto.setPlaceholder("https://… o nombre de archivo");
        foto.setClearButtonVisible(true);
        foto.setWidthFull();

        // Preview imagen
        Image preview = new Image("", "Vista previa");
        preview.setWidth("100%");
        preview.getStyle()
                .set("border-radius", "12px")
                .set("background", "rgba(0,0,0,.04)")
                .set("object-fit", "cover");
        Div previewWrap = new Div(new Paragraph("Vista previa"), preview);
        previewWrap.addClassName("preview-card");

        foto.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        foto.addValueChangeListener(e -> {
            String v = e.getValue() == null ? "" : e.getValue().trim();
            if (v.isBlank()) { preview.setSrc(""); return; }
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
        });

        /* ======== INGREDIENTES ======== */
        VerticalLayout ingHeader = new VerticalLayout();
        ingHeader.setPadding(false);
        ingHeader.setSpacing(false);
        ingHeader.getStyle().set("margin-top", "8px");

        HorizontalLayout rowHeader = new HorizontalLayout();
        rowHeader.setWidthFull();
        rowHeader.setAlignItems(Alignment.CENTER);

        H3 ingTitle = new H3("Ingredientes del producto");
        ingTitle.getStyle().set("margin", "0");

        Button addIng = new Button("Añadir ingrediente", VaadinIcon.PLUS.create());
        addIng.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addIng.addClickListener(e -> ingredientesList.add(createIngredientRow()));

        rowHeader.add(ingTitle);
        rowHeader.expand(ingTitle);
        rowHeader.add(addIng);

        Hr sepIng = new Hr(); sepIng.getStyle().set("margin", "10px 0 12px 0");
        ingHeader.add(rowHeader, sepIng);

        ingredientesList.setPadding(false);
        ingredientesList.setSpacing(true);
        ingredientesList.addClassName("ing-box");
        ingredientesList.add(createIngredientRow()); // primera fila

        // indicadores/ayudas
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

        // distribución
        form.add(nombre, descripcion, stock, precio, foto, ingHeader, ingredientesList, previewWrap);
        form.setColspan(nombre, 2);
        form.setColspan(descripcion, 2);
        form.setColspan(foto, 2);
        form.setColspan(ingHeader, 2);
        form.setColspan(ingredientesList, 2);
        form.setColspan(previewWrap, 2);

        // ====== acciones ======
        Hr sepBottom = new Hr(); sepBottom.getStyle().set("margin", "14px 0");

        Button cancelar = new Button("Cancelar", new Icon(VaadinIcon.ARROW_LEFT));
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelar.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/duennopanel")));

        Button limpiar = new Button("Limpiar", new Icon(VaadinIcon.ERASER));
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        limpiar.addClickListener(e -> {
            nombre.clear(); descripcion.clear(); stock.clear(); precio.clear(); foto.clear();
            preview.setSrc("");
            ingredientesList.removeAll();
            ingredientesList.add(createIngredientRow());
        });

        Button crear = new Button("Crear producto", new Icon(VaadinIcon.PLUS_CIRCLE));
        crear.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        crear.addClickShortcut(Key.ENTER);

        HorizontalLayout actions = new HorizontalLayout(cancelar, limpiar, crear);
        actions.addClassName("action-bar");
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        /* ============ BINDER ============ */
        Binder<Producto> binder = new Binder<>(Producto.class);

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

        /* ============ GUARDAR ============ */
        crear.addClickListener(e -> {
            try {
                // 1) Volcar el formulario al POJO
                Producto producto = new Producto();
                binder.writeBean(producto);

                // 2) Guardar el producto (ya queda managed y con id)
                producto = productoRepository.save(producto);

                // 3) Construir relaciones con referencias managed (no objetos detached del ComboBox)
                List<Producto_Ingrediente> relaciones = readIngredientRowsManaged(producto);

                if (relaciones.isEmpty()) {
                    Notification n = Notification.show("Añade al menos un ingrediente",
                            2500, Notification.Position.MIDDLE);
                    n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    return;
                }

                // 4) Guardar relaciones una sola vez
                productoIngredienteRepository.saveAll(relaciones);

                // 5) OK
                Notification n = Notification.show("Producto creado correctamente",
                        2500, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // 6) Limpiar UI
                nombre.clear(); descripcion.clear(); stock.clear(); precio.clear(); foto.clear();
                preview.setSrc("");
                ingredientesList.removeAll();
                ingredientesList.add(createIngredientRow());

            } catch (ValidationException ex) {
                Notification n = Notification.show("Revisa los campos del formulario",
                        3000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            } catch (IllegalArgumentException ex) {
                Notification n = Notification.show(String.valueOf(ex.getMessage()),
                        3000, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });


        // montaje en el contenedor centrado
        card.add(blockTitle, sepTop, form, sepBottom, actions);
        page.add(hero, card);
    }

    /* ------------ fila de ingrediente: GRID 4 columnas ------------ */
    private Div createIngredientRow() {
        List<Ingrediente> all = ingredienteRepository.findAll();
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
        remove.addClickListener(e -> {
            ingredientesList.remove((Div) remove.getParent().get());
        });

        Div row = new Div(cb, qty, unit, remove);
        row.addClassName("ing-row"); // CSS grid
        return row;
    }

    private List<Producto_Ingrediente> readIngredientRowsManaged(Producto productoManaged) {
        // Obtén solo las filas reales (las que tienen la clase ing-row)
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

            Ingrediente ingredienteManaged =
                    ingredienteRepository.findById(sel.getId())
                            .orElseThrow(() -> new IllegalArgumentException("Ingrediente inexistente"));

            Producto_Ingrediente pi = new Producto_Ingrediente();
            pi.setProducto(productoManaged);          // el producto ya es managed (acabamos de guardarlo)
            pi.setIngrediente(ingredienteManaged);    // referencia managed por id
            pi.setCantidad(cantidad);
            pi.setUnidad(unidad);

            out.add(pi);
        }
        return out;
    }


    /* Lee las filas y crea entidades de unión */
    private List<Producto_Ingrediente> readIngredientRows(Producto producto) {
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

            Ingrediente ing = cb.getValue();
            BigDecimal cantidad = qty.getValue();
            String unidad = unit.getValue();

            if (ing == null && (cantidad == null || BigDecimal.ZERO.compareTo(cantidad) == 0) &&
                    (unidad == null || unidad.isBlank())) continue;

            if (ing == null) throw new IllegalArgumentException("Hay una fila sin ingrediente.");
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("La cantidad de " + ing.getNombre() + " debe ser ≥ 0.");
            if (unidad == null || unidad.isBlank())
                throw new IllegalArgumentException("La unidad de " + ing.getNombre() + " es obligatoria.");
            if (unidad.length() > 8)
                throw new IllegalArgumentException("La unidad para " + ing.getNombre() + " supera 8 caracteres.");
            if (!vistos.add(ing.getId()))
                throw new IllegalArgumentException("Ingrediente repetido: " + ing.getNombre());

            Producto_Ingrediente pi = new Producto_Ingrediente();
            pi.setProducto(producto);
            pi.setIngrediente(ing);
            pi.setCantidad(cantidad);
            pi.setUnidad(unidad);
            out.add(pi);
        }
        return out;
    }

}

