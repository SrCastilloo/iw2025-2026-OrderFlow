package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.ProductoTipo;
import es.uca.orderflow.business.services.GestionarMenu;
import es.uca.orderflow.business.services.GestionarProducto;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Route("/backoffice/menus/crear")
@PageTitle("Crear menú")
@AnonymousAllowed
public class CrearMenuView extends VerticalLayout {

    private final GestionarMenu gestionarMenu;
    private final GestionarProducto gestionarProducto;

    private final Grid<Producto> grid = new Grid<>(Producto.class, false);
    private final Map<Long, Integer> cantidades = new HashMap<>();

    public CrearMenuView(GestionarMenu gestionarMenu, GestionarProducto gestionarProducto) {
        this.gestionarMenu = gestionarMenu;
        this.gestionarProducto = gestionarProducto;

        setWidthFull();
        setMaxWidth("1100px");
        getStyle().set("margin", "0 auto");
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Crear menú");
        add(title);

        TextField nombre = new TextField("Nombre");
        nombre.setWidthFull();
        nombre.setRequired(true);

        TextArea descripcion = new TextArea("Descripción");
        descripcion.setWidthFull();

        BigDecimalField precio = new BigDecimalField("Precio");
        precio.setWidthFull();
        precio.setRequired(true);

        TextField foto = new TextField("Foto (URL o ruta)");
        foto.setWidthFull();

        add(nombre, descripcion, precio, foto);

        add(new H3("Selecciona productos y cantidades"));

        configurarGridProductos();
        cargarProductos();

        add(grid);

        Button guardar = new Button("Guardar menú");
        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.addClickListener(e -> {
            // Validaciones mínimas
            if (nombre.isEmpty() || precio.isEmpty()) {
                Notification n = Notification.show("Nombre y precio son obligatorios.");
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // Construir el Map final con cantidades > 0
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
                        foto.getValue(),
                        productosConCantidad
                );

                Notification.show("Menú creado correctamente.");
                // limpiar
                nombre.clear();
                descripcion.clear();
                precio.clear();
                foto.clear();
                cantidades.clear();
                grid.getDataProvider().refreshAll();

            } catch (Exception ex) {
                Notification n = Notification.show("No se pudo crear el menú: " + ex.getMessage());
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        add(guardar);
    }

    private void configurarGridProductos() {
        grid.addColumn(Producto::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Producto::getNombre).setHeader("Producto").setFlexGrow(1);
        grid.addColumn(p -> p.getPrecio() == null ? "—" : p.getPrecio().toPlainString()).setHeader("Precio").setAutoWidth(true);

        // Columna cantidad editable
        grid.addComponentColumn(p -> {
            IntegerField qty = new IntegerField();
            qty.setMin(0);
            qty.setStepButtonsVisible(true);
            qty.setWidth("120px");

            // valor inicial (si ya se tocó antes)
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
