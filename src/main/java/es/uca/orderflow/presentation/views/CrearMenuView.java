package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.services.GestionarMenu;
import es.uca.orderflow.business.services.GestionarProducto;

import java.io.InputStream;
import java.nio.file.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Route("/backoffice/menus/crear")
@PageTitle("Crear menú")
@AnonymousAllowed
@CssImport("./styles/create-product.css") // Reutiliza el CSS del crear producto
public class CrearMenuView extends VerticalLayout {

    private final GestionarMenu gestionarMenu;
    private final GestionarProducto gestionarProducto;

    private final Grid<Producto> grid = new Grid<>(Producto.class, false);
    private final Map<Long, Integer> cantidades = new HashMap<>();

    public CrearMenuView(GestionarMenu gestionarMenu, GestionarProducto gestionarProducto) {
        this.gestionarMenu = gestionarMenu;
        this.gestionarProducto = gestionarProducto;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(Alignment.CENTER);

        // Fondo igual que crear producto (consistencia visual)
        getStyle().set("background",
                "radial-gradient(1000px 500px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                        "radial-gradient(900px 450px at 110% 8%, rgba(255,120,90,.28), transparent 60%)," +
                        "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)");

        /* ================== HERO ================== */
        Icon heroIcon = VaadinIcon.COFFEE.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 title = new H1("Crear un nuevo menú");
        title.getStyle().set("margin", "0").set("letter-spacing", "-0.02em");

        Paragraph subtitle = new Paragraph("Define los datos del menú, añade productos y confirma para publicarlo.");
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

        H3 blockTitle = new H3("Datos básicos del menú");
        blockTitle.getStyle().set("margin", "0 0 10px 0");

        Hr sepTop = new Hr();
        sepTop.getStyle().set("margin", "10px 0 18px 0");

        /* ================== CAMPOS ================== */
        TextField nombre = new TextField("Nombre");
        nombre.setPrefixComponent(new Icon(VaadinIcon.TAG));
        nombre.setWidthFull();
        nombre.setRequired(true);
        nombre.setRequiredIndicatorVisible(true);
        nombre.setClearButtonVisible(true);
        nombre.setPlaceholder("Nombre del menú");

        TextArea descripcion = new TextArea("Descripción");
        descripcion.setWidthFull();
        descripcion.setClearButtonVisible(true);
        descripcion.setPlaceholder("Descripción breve del menú");

        BigDecimalField precio = new BigDecimalField("Precio");
        precio.setPrefixComponent(new Icon(VaadinIcon.EURO));
        precio.setWidthFull();
        precio.setRequired(true);
        precio.setRequiredIndicatorVisible(true);
        precio.setPlaceholder("0,00");

        /* ================== FOTO (UPLOAD + PREVIEW) ================== */
        TextField fotoHidden = new TextField();
        fotoHidden.setVisible(false);
        fotoHidden.setWidthFull();

        Image preview = new Image("", "Vista previa");
        preview.setWidth("100%");
        preview.getStyle()
                .set("border-radius", "12px")
                .set("background", "rgba(0,0,0,.04)")
                .set("object-fit", "cover");

        Div previewWrap = new Div(new Paragraph("Vista previa"), preview);
        previewWrap.addClassName("preview-card");

        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
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

        H4 imgTitle = new H4("Imagen del menú");
        imgTitle.getStyle().set("margin", "0 0 6px 0");
        Paragraph imgHelp = new Paragraph("Selecciona una imagen (.jpg, .jpeg, .png, .webp). Máx 5MB.");
        imgHelp.getStyle().set("margin", "0 0 10px 0").set("opacity", "0.8");
        uploadWrap.add(imgTitle, imgHelp, upload);

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

                String storedName = UUID.randomUUID() + extension;
                Path dest = dir.resolve(storedName);

                try (InputStream in = buffer.getInputStream()) {
                    Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
                }

                String publicPath = "/product-photos/" + storedName;
                fotoHidden.setValue(publicPath);

                String ctx = (VaadinService.getCurrentRequest() != null)
                        ? VaadinService.getCurrentRequest().getContextPath()
                        : "";
                preview.setSrc(ctx + publicPath + "?t=" + System.currentTimeMillis());

                Notification.show("Imagen guardada", 2000, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            } catch (Exception ex) {
                Notification.show("Error guardando imagen: " + ex.getMessage(), 3500, Notification.Position.MIDDLE)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        upload.addFailedListener(e -> {
            Notification.show("Error subiendo la imagen", 3500, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            upload.clearFileList();
        });

        /* ================== GRID PRODUCTOS ================== */
        H3 productosTitle = new H3("Productos del menú");
        productosTitle.getStyle().set("margin", "10px 0 0 0");

        configurarGridProductos();
        cargarProductos();
        grid.setWidthFull();
        grid.getStyle()
                .set("border-radius", "14px")
                .set("overflow", "hidden")
                .set("background", "rgba(255,255,255,.65)")
                .set("box-shadow", "0 10px 26px rgba(15,23,42,.08)")
                .set("border", "1px solid rgba(15,23,42,.08)");

        /* ================== ACCIONES ================== */
        Hr sepBottom = new Hr();
        sepBottom.getStyle().set("margin", "14px 0");

        Button volver = new Button("Volver", new Icon(VaadinIcon.ARROW_LEFT));
        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volver.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/duennopanel")));

        Button limpiar = new Button("Limpiar", new Icon(VaadinIcon.ERASER));
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        limpiar.addClickListener(e -> {
            nombre.clear();
            descripcion.clear();
            precio.clear();

            upload.clearFileList();
            fotoHidden.clear();
            preview.setSrc("");

            cantidades.clear();
            grid.getDataProvider().refreshAll();
        });

        Button guardar = new Button("Crear menú", new Icon(VaadinIcon.PLUS_CIRCLE));
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.addClickShortcut(Key.ENTER);

        guardar.addClickListener(e -> {
            if (nombre.isEmpty() || precio.isEmpty()) {
                Notification n = Notification.show("Nombre y precio son obligatorios.");
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Si quieres obligar imagen, descomenta:
            // if (fotoHidden.isEmpty()) {
            //     Notification n = Notification.show("Sube una imagen para el menú.");
            //     n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            //     return;
            // }

            Map<Long, Integer> productosConCantidad = cantidades.entrySet().stream()
                    .filter(en -> en.getValue() != null && en.getValue() > 0)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

            if (productosConCantidad.isEmpty()) {
                Notification n = Notification.show("Debes añadir al menos un producto al menú.");
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            try {
                gestionarMenu.crearMenu(
                        nombre.getValue().trim(),
                        descripcion.getValue(),
                        precio.getValue(),
                        fotoHidden.getValue(),
                        productosConCantidad
                );

                Notification n = Notification.show("Menú creado correctamente.", 2500, Notification.Position.MIDDLE);
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                // limpiar
                nombre.clear();
                descripcion.clear();
                precio.clear();

                upload.clearFileList();
                fotoHidden.clear();
                preview.setSrc("");

                cantidades.clear();
                grid.getDataProvider().refreshAll();

            } catch (Exception ex) {
                Notification n = Notification.show("No se pudo crear el menú: " + ex.getMessage());
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        HorizontalLayout actions = new HorizontalLayout(volver, limpiar, guardar);
        actions.addClassName("action-bar");
        actions.setWidthFull();
        actions.setJustifyContentMode(HorizontalLayout.JustifyContentMode.END);

        /* ================== MONTAJE CARD ================== */
        card.add(
                blockTitle, sepTop,
                nombre, descripcion, precio,
                uploadWrap, quitarImagen, fotoHidden,
                previewWrap,
                new Hr(),
                productosTitle,
                grid,
                sepBottom,
                actions
        );

        page.add(hero, card);
    }

    private void configurarGridProductos() {
        grid.removeAllColumns();

        grid.addColumn(Producto::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Producto::getNombre).setHeader("Producto").setFlexGrow(1);
        grid.addColumn(p -> p.getPrecio() == null ? "—" : p.getPrecio().toPlainString())
                .setHeader("Precio").setAutoWidth(true);

        grid.addComponentColumn(p -> {
            IntegerField qty = new IntegerField();
            qty.setMin(0);
            qty.setStepButtonsVisible(true);
            qty.setWidth("120px");

            Integer current = cantidades.getOrDefault(p.getId(), 0);
            qty.setValue(current);

            qty.addValueChangeListener(e -> {
                Integer v = e.getValue();
                if (v == null) v = 0;
                cantidades.put(p.getId(), v);
            });

            return qty;
        }).setHeader("Cantidad").setAutoWidth(true);
    }

    private void cargarProductos() {
        List<Producto> productos = gestionarProducto.consultarSoloProductos();
        grid.setItems(productos);
    }
}
