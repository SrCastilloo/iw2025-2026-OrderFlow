package es.uca.orderflow.presentation.components;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
        nombre.clear();
        descripcion.clear();
        stock.clear();
        precio.clear();

        upload.clearFileList();
        fotoHidden.clear();
        preview.setSrc("");

        ingredientesList.removeAll();
        ingredientesList.add(createIngredientRow());
        binder.readBean(null);
    }

    private void buildFields() {
        // básicos
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

        fotoHidden.setVisible(false);

        // preview
        preview.setWidth("100%");
        preview.getStyle()
                .set("border-radius", "12px")
                .set("background", "rgba(0,0,0,.04)")
                .set("object-fit", "cover");

        Div previewWrap = new Div(new Paragraph("Vista previa"), preview);
        previewWrap.addClassName("preview-card");

        // upload
        upload.setMaxFiles(1);
        upload.setAutoUpload(true);
        upload.setDropAllowed(true);
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/webp");
        upload.getElement().setProperty("accept", ".jpg,.jpeg,.png,.webp");
        upload.setMaxFileSize(5 * 1024 * 1024);

        Div uploadWrap = new Div();
        uploadWrap.getStyle()
                .set("border-radius", "14px")
                .set("padding", "14px")
                .set("border", "1px dashed var(--lumo-contrast-20pct)")
                .set("background", "rgba(255,255,255,.55)");
        uploadWrap.add(new H4("Imagen del producto"),
                new Paragraph("Selecciona una imagen (.jpg, .jpeg, .png, .webp). Máx 5MB."),
                upload);

        Button quitarImagen = new Button("Quitar imagen", VaadinIcon.CLOSE_SMALL.create());
        quitarImagen.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        quitarImagen.addClickListener(ev -> {
            upload.clearFileList();
            fotoHidden.clear();
            preview.setSrc("");
        });

        upload.addSucceededListener(e -> {
            try {
                String fileName = e.getFileName();
                String extension = fileName.substring(fileName.lastIndexOf(".")).toLowerCase();

                Path dir = Paths.get(System.getProperty("user.dir"), "frontend-resources", "products")
                        .toAbsolutePath().normalize();
                if (!Files.exists(dir)) Files.createDirectories(dir);

                String storedName = UUID.randomUUID().toString() + extension;
                Path dest = dir.resolve(storedName);

                try (InputStream in = buffer.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }

                String publicPath = "/product-photos/" + storedName;
                fotoHidden.setValue(publicPath);

                String ctx = (VaadinService.getCurrentRequest() != null)
                        ? VaadinService.getCurrentRequest().getContextPath() : "";
                preview.setSrc(ctx + publicPath + "?t=" + System.currentTimeMillis());

                Notification.show("Imagen guardada", 2000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFailedListener(e -> {
            Notification.show("Error subiendo la imagen", 3500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            upload.clearFileList();
        });

        // ingredientes
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

        ingredientesList.setPadding(false);
        ingredientesList.setSpacing(true);
        ingredientesList.addClassName("ing-box");
        ingredientesList.add(createIngredientRow());

        // form layout
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("820px", 2)
        );

        form.add(nombre, descripcion, stock, precio,
                uploadWrap, quitarImagen, fotoHidden,
                ingHeader, ingredientesList, previewWrap);

        form.setColspan(nombre, 2);
        form.setColspan(descripcion, 2);
        form.setColspan(uploadWrap, 2);
        form.setColspan(quitarImagen, 2);
        form.setColspan(fotoHidden, 2);
        form.setColspan(ingHeader, 2);
        form.setColspan(ingredientesList, 2);
        form.setColspan(previewWrap, 2);

        add(form);
    }

    private void buildBinder() {
        nombre.setRequiredIndicatorVisible(true);
        descripcion.setRequiredIndicatorVisible(true);
        stock.setRequiredIndicatorVisible(true);
        precio.setRequiredIndicatorVisible(true);

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

    private Div createIngredientRow() {
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

        ComboBox<String> unit = new ComboBox<>("Unidad");
        unit.setItems(unidades);
        unit.setPlaceholder("g/ml/…");
        unit.setWidthFull();

        Button remove = new Button(VaadinIcon.TRASH.create());
        remove.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        remove.getElement().getStyle().set("align-self", "end");
        remove.addClickListener(e -> ingredientesList.remove((Div) remove.getParent().get()));

        Div row = new Div(cb, qty, unit, remove);
        row.addClassName("ing-row");
        return row;
    }
}
