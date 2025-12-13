package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.business.services.GestionarMesa;
import es.uca.orderflow.business.services.GestionarPedido;
import es.uca.orderflow.business.services.PaymentMethod;
import es.uca.orderflow.persistence.data.ClienteRepository;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.util.*;

@PageTitle("Crear Pedido")
@Route("/backoffice/crearpedido")
@AnonymousAllowed // de momento
public class CrearPedidoView extends VerticalLayout {

    private final GestionarPedido gestionarPedido;
    private final ClienteRepository clienteRepository;
    private final Empleado recepcionista;
    private ComboBox<Mesa> mesaComboBox;  // ComboBox para seleccionar la mesa
    private final GestionarMesa gestionarMesa;
    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));


    private Map<Producto, Integer> carrito = new HashMap<>(); // Carrito temporal
    private Grid<Map.Entry<Producto, Integer>> gridCarrito;
    private ComboBox<Cliente> clienteComboBox;
    private TextField nombrePedidoTextField; // Para clientes no registrados (ej: Mesa 3)
    private VerticalLayout summaryContainer;
    private VerticalLayout infoCol; // Referencia para actualizar el resumen

    // Constante para la clave del carrito en sesión
    private static final String RECEPCIONISTA_CART_KEY = "recepcionistaCart";

    @Autowired
    public CrearPedidoView(GestionarPedido gestionarPedido, ClienteRepository clienteRepository,GestionarMesa gestionarMesa) {
        this.gestionarPedido = gestionarPedido;
        this.gestionarMesa = gestionarMesa;
        this.clienteRepository = clienteRepository;
        this.recepcionista = (Empleado) VaadinSession.getCurrent().getAttribute("empleadoLogueado");

        if (recepcionista == null) {
            UI.getCurrent().navigate("/login");
            return;
        }

        Object cartAttribute = VaadinSession.getCurrent().getAttribute(RECEPCIONISTA_CART_KEY);
        if (cartAttribute instanceof Map) {
            // Se realiza un cast seguro, asumiendo que la estructura es Producto -> Integer
            try {
                this.carrito = (Map<Producto, Integer>) cartAttribute;
            } catch (ClassCastException e) {
                // Si el cast falla por alguna razón, se inicializa un nuevo carrito
                this.carrito = new HashMap<>();
                VaadinSession.getCurrent().setAttribute(RECEPCIONISTA_CART_KEY, this.carrito);
            }
        } else {
            VaadinSession.getCurrent().setAttribute(RECEPCIONISTA_CART_KEY, this.carrito);
        }


        // Inicializar componentes que serán usados por añadirProductoACarrito/updateCartView
        gridCarrito = buildCartGrid();

        infoCol = new VerticalLayout();
        infoCol.setWidth("40%");
        infoCol.setPadding(false);
        infoCol.setSpacing(true);

        summaryContainer = buildOrderSummary(); // Llama a buildOrderSummary() que usa 'carrito'

        // 2. Intentar añadir un producto si viene del panel (Ahora es seguro llamar a añadirProductoACarrito)
        Producto productoTemporal = (Producto) VaadinSession.getCurrent().getAttribute("productoAñadirTemporal");
        if (productoTemporal != null) {
            añadirProductoACarrito(productoTemporal);
            VaadinSession.getCurrent().setAttribute("productoAñadirTemporal", null); // Limpiar la sesión
        }

        // ====== LAYOUT GENERAL ======
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.CENTER);
        getStyle().set("background", "var(--lumo-base-color)");

        Div wrapper = new Div();
        wrapper.getStyle()
                .set("width", "100%")
                .set("max-width", "1000px")
                .set("margin", "0 auto")
                .set("padding", "1.5rem");

        // Cabecera
        H1 titulo = new H1("Crear Nuevo Pedido");
        titulo.getStyle().set("color", "var(--lumo-header-text-color)");

        Button volver = new Button("Volver al Panel", VaadinIcon.ARROW_BACKWARD.create(),
                e -> UI.getCurrent().navigate(PanelRecepcionistaView.class));
        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        HorizontalLayout header = new HorizontalLayout(titulo, volver);
        header.setWidthFull();
        header.setJustifyContentMode(JustifyContentMode.BETWEEN);
        header.setAlignItems(Alignment.BASELINE);

        wrapper.add(header);

        HorizontalLayout mainContent = new HorizontalLayout();
        mainContent.setWidthFull();
        mainContent.setSpacing(true);

        infoCol.add(buildClienteSelector());
        // ** Aquí se agrega el ComboBox de selección de mesa **
        mesaComboBox = new ComboBox<>("Seleccionar mesa");
        mesaComboBox.setWidthFull();

        mesaComboBox.setItems(gestionarMesa.obtenerMesasLibres());
        mesaComboBox.setItemLabelGenerator(m -> m.getNombre() + " (LIBRE)");

        // Lógica para seleccionar la mesa
        mesaComboBox.addValueChangeListener(e -> {
            if (e.getValue() != null) {
                Mesa mesaSeleccionada = e.getValue();
                mesaSeleccionada.setEstado(EstadoMesa.OCUPADA);
                gestionarMesa.guardarMesa(mesaSeleccionada); // Guardamos la mesa ocupada
            }
        });

        infoCol.add(mesaComboBox);  // Agregar el ComboBox a la interfaz de usuario
        // Usar summaryContainer ya inicializado arriba
        infoCol.add(summaryContainer);

        mainContent.add(infoCol);

        VerticalLayout cartCol = new VerticalLayout();
        cartCol.setWidth("60%");
        cartCol.setPadding(false);
        cartCol.setSpacing(true);

        H3 productosTitulo = new H3("Productos Seleccionados");
        productosTitulo.getStyle().set("margin-top", "0");
        cartCol.add(productosTitulo);

        cartCol.add(gridCarrito);

        // Botones de Acción
        Button btnConfirmar = new Button("Confirmar Pedido (Pagar en Tienda)", VaadinIcon.CHECK.create(),
                e -> this.confirmarPedido()
        );
        btnConfirmar.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_LARGE);
        btnConfirmar.setWidthFull();

        Button btnVaciar = new Button("Vaciar Carrito", VaadinIcon.TRASH.create(), e -> vaciarCarrito());
        btnVaciar.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout cartActions = new HorizontalLayout(btnConfirmar, btnVaciar);
        cartActions.setWidthFull();
        cartActions.expand(btnConfirmar);

        cartCol.add(cartActions);

        mainContent.add(cartCol);
        wrapper.add(mainContent);
        add(wrapper);

        // Se llama al final para renderizar el estado actual del carrito
        updateCartView();
    }


    private Component buildClienteSelector() {
        Details details = new Details("1. Seleccionar Cliente",
                new Paragraph("Busca un cliente registrado o introduce un nombre para un cliente que realiza el pedido en local (ej: Mesa 3)."));

        clienteComboBox = new ComboBox<>("Cliente Registrado");
        // Asegúrate de que ClienteRepository tiene un findAll() accesible
        clienteComboBox.setItems(clienteRepository.findAll());
        clienteComboBox.setItemLabelGenerator(c -> c.getNombre() + " " + c.getApellidos() + " (" + c.getCorreo() + ")");
        clienteComboBox.setClearButtonVisible(true);
        clienteComboBox.setWidthFull();

        // Desactiva el campo de nombre si se selecciona un cliente, y viceversa
        clienteComboBox.addValueChangeListener(e -> {
            boolean selected = e.getValue() != null;
            nombrePedidoTextField.setEnabled(!selected);
            if(selected) nombrePedidoTextField.clear();
        });

        nombrePedidoTextField = new TextField("Nombre para Pedido (Cliente Invitado/Mesa)");
        nombrePedidoTextField.setPlaceholder("Ej.: Mesa 3 o Juan Pérez (Teléfono)");
        nombrePedidoTextField.setWidthFull();

        // Desactiva el ComboBox si se introduce texto en el campo de invitado
        nombrePedidoTextField.addValueChangeListener(e -> {
            boolean hasText = e.getValue() != null && !e.getValue().isBlank();
            clienteComboBox.setEnabled(!hasText);
            if(hasText) clienteComboBox.clear();
        });


        VerticalLayout content = new VerticalLayout(clienteComboBox, nombrePedidoTextField);
        content.setPadding(false);
        details.setContent(content);
        return details;
    }

    private Grid<Map.Entry<Producto, Integer>> buildCartGrid() {
        Grid<Map.Entry<Producto, Integer>> grid = new Grid<>();
        grid.setWidthFull();
        // grid.setAllRowsVisible(true); // Se controla en updateCartView
        grid.setSelectionMode(Grid.SelectionMode.NONE);


        grid.addColumn(entry -> entry.getKey().getNombre())
                .setHeader("Producto")
                .setFlexGrow(2);

        grid.addColumn(new ComponentRenderer<>(this::buildQuantityControls))
                .setHeader("Cantidad")
                .setFlexGrow(1)
                .setKey("qty");

        grid.addColumn(entry -> euro.format(entry.getKey().getPrecio().multiply(BigDecimal.valueOf(entry.getValue()))))
                .setHeader("Total")
                .setFlexGrow(1);

        grid.addColumn(new ComponentRenderer<>(entry -> {
                    Button remove = new Button(VaadinIcon.TRASH.create(), e -> eliminarProducto(entry.getKey()));
                    remove.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
                    return remove;
                }))
                .setHeader("Eliminar")
                .setFlexGrow(0);

        return grid;
    }

    private Component buildQuantityControls(Map.Entry<Producto, Integer> entry) {
        Producto p = entry.getKey();

        Button minus = new Button(VaadinIcon.MINUS.create(), e -> updateQuantity(p, entry.getValue() - 1));
        minus.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        minus.setEnabled(entry.getValue() > 1);

        Span count = new Span(String.valueOf(entry.getValue()));
        count.getStyle().set("font-weight", "bold");

        Button plus = new Button(VaadinIcon.PLUS.create(), e -> updateQuantity(p, entry.getValue() + 1));
        plus.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);

        HorizontalLayout controls = new HorizontalLayout(minus, count, plus);
        controls.setAlignItems(Alignment.CENTER);
        controls.setSpacing(true);

        return controls;
    }

    private VerticalLayout buildOrderSummary() {
        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        H3 resumenTitulo = new H3("2. Resumen y Total");
        resumenTitulo.getStyle().set("margin-top", "0");
        layout.add(resumenTitulo);

        // Cálculos
        BigDecimal subtotal = carrito.entrySet().stream()
                .map(e -> e.getKey().getPrecio().multiply(BigDecimal.valueOf(e.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        // Asumiendo un tipo fijo de IVA del 21% para el cálculo de ejemplo
        BigDecimal ivaRate = BigDecimal.valueOf(0.21);
        BigDecimal tax = subtotal.multiply(ivaRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.add(tax).setScale(2, RoundingMode.HALF_UP);

        // Componentes de resumen
        layout.add(
                createSummaryRow("Subtotal:", euro.format(subtotal), false),
                createSummaryRow("IVA (21%):", euro.format(tax), false),
                new Hr(),
                createSummaryRow("Total Final:", euro.format(total), true)
        );
        return layout;
    }

    private HorizontalLayout createSummaryRow(String label, String value, boolean isTotal) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(JustifyContentMode.BETWEEN);

        Span labelSpan = new Span(label);
        Span valueSpan = new Span(value);

        if (isTotal) {
            labelSpan.getStyle().set("font-weight", "bold").set("font-size", "1.1rem");
            valueSpan.getStyle().set("font-weight", "800").set("font-size", "1.3rem").set("color", "var(--lumo-primary-text-color)");
        } else {
            labelSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
            valueSpan.getStyle().set("font-weight", "500");
        }

        row.add(labelSpan, valueSpan);
        return row;
    }

    // =========================================================
    // === LÓGICA DEL CARRITO
    // =========================================================

    private void añadirProductoACarrito(Producto p) {
        carrito.put(p, carrito.getOrDefault(p, 0) + 1);
        VaadinSession.getCurrent().setAttribute(RECEPCIONISTA_CART_KEY, this.carrito);
        // Llamada a updateCartView ahora es segura, ya que gridCarrito está inicializado.
        updateCartView();
        Notification.show(p.getNombre() + " añadido al carrito.", 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    private void eliminarProducto(Producto p) {
        carrito.remove(p);
        VaadinSession.getCurrent().setAttribute(RECEPCIONISTA_CART_KEY, this.carrito);
        updateCartView();
        Notification.show(p.getNombre() + " eliminado del carrito.", 1500, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private void updateQuantity(Producto p, int newQty) {
        if (newQty <= 0) {
            eliminarProducto(p);
            return;
        }
        carrito.put(p, newQty);
        VaadinSession.getCurrent().setAttribute(RECEPCIONISTA_CART_KEY, this.carrito);
        updateCartView();
    }

    private void vaciarCarrito() {
        if (carrito.isEmpty()) return;
        carrito.clear();
        // CORRECCIÓN 1: Usar setAttribute(key, null) en lugar de removeAttribute
        VaadinSession.getCurrent().setAttribute(RECEPCIONISTA_CART_KEY, null);
        updateCartView();
        Notification.show("Carrito vaciado.", 2000, Notification.Position.BOTTOM_END)
                .addThemeVariants(NotificationVariant.LUMO_WARNING);
    }

    private void updateCartView() {
        // Actualiza el Grid del carrito
        // gridCarrito nunca es null en este punto gracias a la corrección del constructor.
        gridCarrito.setItems(carrito.entrySet());

        // Actualiza el Resumen
        if (summaryContainer != null) {
            // El resumen debe ser reconstruido para reflejar los nuevos precios
            infoCol.remove(summaryContainer);
            summaryContainer = buildOrderSummary();
            infoCol.add(summaryContainer);
        }

        if (carrito.isEmpty()) {
            gridCarrito.setAllRowsVisible(false);
            // Mantener el enfoque en la compatibilidad y funcionalidad principal.
            // Si setEmptyTextComponent o setNoRowsText no existen, simplemente no se verá el texto.
        } else {
            gridCarrito.setAllRowsVisible(true);
        }
    }

    // =========================================================
    // === LÓGICA DE CONFIRMACIÓN (Llama al Servicio)
    // =========================================================

    /*
    private void confirmarPedido() {
        if (carrito.isEmpty()) {
            Notification.show("El carrito está vacío. Añade productos para crear el pedido.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Cliente clienteSeleccionado = clienteComboBox.getValue();
        String nombreInvitado = nombrePedidoTextField.getValue();

        if (clienteSeleccionado == null && (nombreInvitado == null || nombreInvitado.isBlank())) {
            Notification.show("Debes seleccionar un cliente registrado o ingresar un nombre para el cliente invitado.", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            Cliente clienteParaPedido = clienteSeleccionado;
            String mensajeExito;

            // Si no hay cliente seleccionado, creamos un Cliente "Invitado"
            if (clienteParaPedido == null) {
                // Crear un Cliente 'Invitado' con el nombre del TextField
                clienteParaPedido = new Cliente();
                // Asignamos el nombre/identificador introducido
                clienteParaPedido.setNombre(nombreInvitado);
                clienteParaPedido.setApellidos("(Invitado)"); // Identificar como invitado
                // Correo único temporal
                clienteParaPedido.setCorreo("invitado_" + UUID.randomUUID().toString().substring(0, 8) + "@orderflow.inv");
                clienteParaPedido.setContrasena("");
                clienteParaPedido = clienteRepository.save(clienteParaPedido);
                mensajeExito = "Pedido creado para Cliente Invitado: " + nombreInvitado + ".";

            } else {
                mensajeExito = "Pedido creado con éxito para " + clienteParaPedido.getNombre() + " " + clienteParaPedido.getApellidos() + ".";
            }

            gestionarPedido.crearPedidoRecepcionista(
                    clienteParaPedido,
                    carrito,
                    PaymentMethod.EFECTIVO,
                    "PAID"
            );

            Notification.show(mensajeExito, 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            vaciarCarrito();
            UI.getCurrent().navigate(PanelRecepcionistaView.class); // Volver al panel

        } catch (Exception e) {
            Notification.show("Error al crear el pedido: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            e.printStackTrace();
        }
    }

     */

    private void confirmarPedido() {
        if (carrito.isEmpty()) {
            Notification.show("El carrito está vacío. Añade productos para crear el pedido.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        Cliente clienteSeleccionado = clienteComboBox.getValue();
        String nombreInvitado = nombrePedidoTextField.getValue();
        Mesa mesaSeleccionada = mesaComboBox.getValue();  // Obtener la mesa seleccionada

        if (clienteSeleccionado == null && (nombreInvitado == null || nombreInvitado.isBlank())) {
            Notification.show("Debes seleccionar un cliente registrado o ingresar un nombre para el cliente invitado.", 4000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        if (mesaSeleccionada == null) {
            Notification.show("Debes seleccionar una mesa para el pedido.", 3000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            Cliente clienteParaPedido = clienteSeleccionado;
            String mensajeExito;

            if (clienteParaPedido == null) {
                clienteParaPedido = new Cliente();
                clienteParaPedido.setNombre(nombreInvitado);
                clienteParaPedido.setApellidos("(Invitado)");
                clienteParaPedido.setCorreo("invitado_" + UUID.randomUUID().toString().substring(0, 8) + "@orderflow.inv");
                clienteParaPedido.setContrasena("");
                clienteParaPedido = clienteRepository.save(clienteParaPedido);
                mensajeExito = "Pedido creado para Cliente Invitado: " + nombreInvitado + ".";
            } else {
                mensajeExito = "Pedido creado con éxito para " +
                        clienteParaPedido.getNombre() + " " + clienteParaPedido.getApellidos() + ".";
            }

            // 1) Crear el pedido usando el nombre de la mesa como dirección
            gestionarPedido.crearPedidoRecepcionista(
                    clienteParaPedido,
                    carrito,
                    mesaSeleccionada.getNombre(),   // dirección = nombre mesa
                    PaymentMethod.EFECTIVO,
                    "PAID"
            );

            // 2) Marcar la mesa como OCUPADA
            gestionarMesa.marcarMesaOcupada(mesaSeleccionada.getId());

            Notification.show(mensajeExito, 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            vaciarCarrito();
            UI.getCurrent().navigate(PanelRecepcionistaView.class);

        } catch (Exception e) {
            Notification.show("Error al crear el pedido: " + e.getMessage(), 5000, Notification.Position.MIDDLE)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            e.printStackTrace();
        }
    }


}