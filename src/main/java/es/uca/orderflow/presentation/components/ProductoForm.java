package es.uca.orderflow.presentation.components;

import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.validator.StringLengthValidator;
import com.vaadin.flow.server.VaadinService;

import es.uca.orderflow.business.entities.Ingrediente;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.business.services.GestionarIngredientes;

import java.io.InputStream;
import java.math.BigDecimal;
import java.nio.file.*;
import java.util.*;

public class ProductoForm extends VerticalLayout {

    private static final int MAX_UPLOAD_BYTES = 5 * 1024 * 1024;
    private static final List<String> UNIDADES = List.of("g", "kg", "ml", "l", "u");

    private final transient GestionarIngredientes gestionarIngredientes;

    private final Binder<Producto> binder = new Binder<>(Producto.class);

    private final TextField nombre = new TextField("Nombre");
    private final TextField descripcion = new TextField("Descripción");
    private final IntegerField stock = new IntegerField("Stock");
    private final BigDecimalField precio = new BigDecimalField("Precio");

    private final TextField fotoHidden = new TextField();
    private final Image preview = new Image("", "Vista previa");

    private final VerticalLayout ingredientesList = new VerticalLayout();
    private final MemoryBuffer buffer = new MemoryBuffer();
    private final Upload upload = new Upload(buffer);

    public ProductoForm(GestionarIngredientes gestionarIngredientes) {
        this.gestionarIngredientes = gestionarIngredientes;

        setPadding(false);
        setSpacing(false);
        setWidthFull();

        buildFields();
        buildBinder();
    }

    public Binder<Producto> getBinder() {
        return binder;
    }

    public Producto buildProducto() throws ValidationException {
        Producto p = new Producto();
        binder.writeBean(p);
        return p;
    }

    public List<Producto_Ingrediente> buildRelaciones(Producto productoManaged) {
        return ProductoIngredienteMapper.buildRelaciones(
                gestionarIngredientes,
                productoManaged,
                ingredientesList.getChildren().toList()
        );
    }

    public void reset() {
        clearBaseFields();
        clearImageFields();
        resetIngredientes();
        binder.readBean(null);
    }

    /* ========================= UI BUILDERS ========================= */

    private void buildFields() {
        configureBaseFields();
        configureHiddenFoto();
        configurePreview();

        Div previewWrap = new Div(new Paragraph("Vista previa"), preview);
        previewWrap.addClassName("preview-card");

        Div uploadWrap = buildUploadWrap();
        Button quitarImagen = buildRemoveImageButton();

        configureIngredientesBlock();

        FormLayout form = buildFormLayout(
                uploadWrap,
                quitarImagen,
                previewWrap
        );

        add(form);
    }

    private FormLayout buildFormLayout(Div uploadWrap, Button quitarImagen, Div previewWrap) {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("820px", 2)
        );

        VerticalLayout ingHeader = buildIngredientesHeader();

        form.add(
                nombre, descripcion, stock, precio,
                uploadWrap, quitarImagen, fotoHidden,
                ingHeader, ingredientesList, previewWrap
        );

        setFullSpan(form, nombre, descripcion, uploadWrap, quitarImagen, fotoHidden, ingHeader, ingredientesList, previewWrap);
        return form;
    }

    private VerticalLayout buildIngredientesHeader() {
        VerticalLayout ingHeader = new VerticalLayout();
        ingHeader.setPadding(false);
        ingHeader.setSpacing(false);

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

        Hr sepIng = new Hr();
        sepIng.getStyle().set("margin", "10px 0 12px 0");

        ingHeader.add(rowHeader, sepIng);
        return ingHeader;
    }

    private Div buildUploadWrap() {
        configureUpload();

        Div uploadWrap = new Div();
        uploadWrap.getStyle()
                .set("border-radius", "14px")
                .set("padding", "14px")
                .set("border", "1px dashed var(--lumo-contrast-20pct)")
                .set("background", "rgba(255,255,255,.55)");

        uploadWrap.add(
                new H4("Imagen del producto"),
                new Paragraph("Selecciona una imagen (.jpg, .jpeg, .png, .webp). Máx 5MB."),
                upload
        );

        wireUploadListeners();
        return uploadWrap;
    }

    private Button buildRemoveImageButton() {
        Button quitarImagen = new Button("Quitar imagen", VaadinIcon.CLOSE_SMALL.create());
        quitarImagen.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        quitarImagen.addClickListener(ev -> clearImageFields());
        return quitarImagen;
    }

    /* ========================= CONFIG HELPERS ========================= */

    private void configureBaseFields() {
        configureTextField(nombre, VaadinIcon.TAG, "Nombre del producto", true);
        configureTextField(descripcion, VaadinIcon.CLIPBOARD_TEXT, "Descripción breve", true);

        stock.setPrefixComponent(new Icon(VaadinIcon.CUBE));
        stock.setSuffixComponent(new Icon(VaadinIcon.EXCHANGE));
        stock.setStep(1);
        stock.setMin(0);
        stock.setPlaceholder("Unidades disponibles");
        stock.setWidthFull();

        precio.setPrefixComponent(new Icon(VaadinIcon.EURO));
        precio.setPlaceholder("0,00");
        precio.setWidthFull();
    }

    private void configureTextField(TextField tf, VaadinIcon icon, String placeholder, boolean clearButton) {
        tf.setPrefixComponent(new Icon(icon));
        tf.setPlaceholder(placeholder);
        tf.setClearButtonVisible(clearButton);
        tf.setWidthFull();
    }

    private void configureHiddenFoto() {
        fotoHidden.setVisible(false);
    }

    private void configurePreview() {
        preview.setWidth("100%");
        preview.getStyle()
                .set("border-radius", "12px")
                .set("background", "rgba(0,0,0,.04)")
                .set("object-fit", "cover");
    }

    private void configureUpload() {
        upload.setMaxFiles(1);
        upload.setAutoUpload(true);
        upload.setDropAllowed(true);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        upload.getElement().setProperty("accept", ".jpg,.jpeg,.png,.webp");
        upload.setMaxFileSize(MAX_UPLOAD_BYTES);
    }

    private void configureIngredientesBlock() {
        ingredientesList.setPadding(false);
        ingredientesList.setSpacing(true);
        ingredientesList.addClassName("ing-box");
        ingredientesList.removeAll();
        ingredientesList.add(createIngredientRow());
    }

    private void setFullSpan(FormLayout form, Component... components) {
        for (Component c : components) form.setColspan(c, 2);
    }

    /* ========================= BINDER ========================= */

    private void buildBinder() {
        setRequiredIndicators(true, nombre, descripcion, stock, precio);

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

        binder.forField(fotoHidden)
                .asRequired("Sube una imagen del producto")
                .withValidator(v -> v != null && v.length() <= 200, "Máximo 200 caracteres")
                .bind(Producto::getFoto, Producto::setFoto);
    }

    @SafeVarargs
    private void setRequiredIndicators(boolean required, TextField... fields) {
        for (TextField f : fields) f.setRequiredIndicatorVisible(required);
    }

    private void setRequiredIndicators(boolean required, IntegerField... fields) {
        for (IntegerField f : fields) f.setRequiredIndicatorVisible(required);
    }

    private void setRequiredIndicators(boolean required, BigDecimalField... fields) {
        for (BigDecimalField f : fields) f.setRequiredIndicatorVisible(required);
    }

    private void setRequiredIndicators(boolean required, TextField tf1, TextField tf2, IntegerField i1, BigDecimalField b1) {
        tf1.setRequiredIndicatorVisible(required);
        tf2.setRequiredIndicatorVisible(required);
        i1.setRequiredIndicatorVisible(required);
        b1.setRequiredIndicatorVisible(required);
    }

    /* ========================= UPLOAD HANDLERS ========================= */

    private void wireUploadListeners() {
        upload.addSucceededListener(e -> {
            try {
                String storedName = storeUploadedImage(buffer.getInputStream(), e.getFileName());
                String publicPath = "/product-photos/" + storedName;

                fotoHidden.setValue(publicPath);
                preview.setSrc(contextPath() + publicPath + cacheBust());

                notifySuccess("Imagen guardada", 2000);

            } catch (Exception ex) {
                notifyError("Error: " + ex.getMessage(), 3500);
            }
        });

        upload.addFailedListener(e -> {
            notifyError("Error subiendo la imagen", 3500);
            upload.clearFileList();
        });
    }

    private String storeUploadedImage(InputStream in, String originalFileName) throws Exception {
        String extension = safeExtension(originalFileName);
        Path dir = ensureProductsDir();

        String storedName = UUID.randomUUID() + extension;
        Path dest = dir.resolve(storedName);

        try (InputStream input = in) {
            Files.copy(input, dest, StandardCopyOption.REPLACE_EXISTING);
        }
        return storedName;
    }

    private Path ensureProductsDir() throws Exception {
        Path dir = Paths.get(System.getProperty("user.dir"), "frontend-resources", "products")
                .toAbsolutePath()
                .normalize();
        if (!Files.exists(dir)) Files.createDirectories(dir);
        return dir;
    }

    private String safeExtension(String fileName) {
        if (fileName == null) return ".png";
        int idx = fileName.lastIndexOf('.');
        if (idx < 0 || idx == fileName.length() - 1) return ".png";
        return fileName.substring(idx).toLowerCase();
    }

    private String contextPath() {
        return (VaadinService.getCurrentRequest() != null)
                ? VaadinService.getCurrentRequest().getContextPath()
                : "";
    }

    private String cacheBust() {
        return "?t=" + System.currentTimeMillis();
    }

    private void notifySuccess(String msg, int ms) {
        Notification.show(msg, ms, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void notifyError(String msg, int ms) {
        Notification.show(msg, ms, Notification.Position.MIDDLE)
                .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    /* ========================= INGREDIENT ROW ========================= */

    private Div createIngredientRow() {
        List<Ingrediente> all = gestionarIngredientes.obtenerIngredientes();

        ComboBox<Ingrediente> cb = new ComboBox<>("Ingrediente");
        cb.setItems(all);
        cb.setItemLabelGenerator(Ingrediente::getNombre);
        cb.setPlaceholder("Elige ingrediente");
        cb.setWidthFull();

        BigDecimalField qty = new BigDecimalField("Cantidad");
        qty.setPlaceholder("0");
        qty.setHelperText("≥ 0");
        qty.setWidthFull();

        ComboBox<String> unit = new ComboBox<>("Unidad");
        unit.setItems(UNIDADES);
        unit.setPlaceholder("g/ml/…");
        unit.setWidthFull();

        Button remove = new Button(VaadinIcon.TRASH.create());
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        remove.getElement().getStyle().set("align-self", "end");
        remove.addClickListener(e -> ingredientesList.remove((Div) remove.getParent().orElse(null)));

        Div row = new Div(cb, qty, unit, remove);
        row.addClassName("ing-row");
        return row;
    }

    /* ========================= RESET HELPERS ========================= */

    private void clearBaseFields() {
        nombre.clear();
        descripcion.clear();
        stock.clear();
        precio.clear();
    }

    private void clearImageFields() {
        upload.clearFileList();
        fotoHidden.clear();
        preview.setSrc("");
    }

    private void resetIngredientes() {
        ingredientesList.removeAll();
        ingredientesList.add(createIngredientRow());
    }
}
