package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Empleado; // Necesitas la entidad Empleado
import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.business.services.GestionarPedido;
import es.uca.orderflow.business.services.PedidoEstado;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

@PageTitle("Panel Repartidor")
@Route("/backoffice/repartidor")
@AnonymousAllowed
@CssImport("./styles/empleados.css")
public class PanelRepartidorView extends VerticalLayout {

    private final GestionarPedido gestionarPedido;
    private final Empleado repartidor;
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private final Div filterBar = new Div();
    private PedidoEstado estadoFiltro = PedidoEstado.LISTO_REPARTO;

    public PanelRepartidorView(GestionarPedido gestionarPedido) {
        this.gestionarPedido = gestionarPedido;

        // --- Obtener Repartidor Logueado ---
        Object empleadoObj = VaadinSession.getCurrent().getAttribute("empleadoLogueado");
        if (empleadoObj instanceof Empleado) {
            this.repartidor = (Empleado) empleadoObj;
        } else {
            // Si no hay sesión o no es un Empleado (Repartidor), redirigir o mostrar error.
            UI.getCurrent().navigate("/login");
            this.repartidor = null; // Asignación de seguridad, aunque la navegación detendrá la ejecución
            return;
        }

        /* ======== ESTILO GENERAL DEL ROOT ======== */
        setId("cook-root");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setHeightFull();

        getStyle().set("background",
                "radial-gradient(1200px 600px at 50% -200px, rgba(255,255,255,.75), rgba(255,255,255,0))," +
                        "linear-gradient(180deg,#fefefe 0%, #f7f7f7 40%, #ffffff 100%)");

        createGrid();

        add(buildTopBar(), buildFilterChips(), buildWrapper());

        loadData();
    }

    /* ========================= TOP BAR (Sticky) ========================= */

    private Component buildTopBar() {
        Div band = new Div();
        band.setWidthFull();
        band.setId("cook-band");

        band.getStyle()
                .set("position", "sticky")
                .set("top", "0")
                .set("z-index", "50")
                .set("background", "linear-gradient(180deg, rgba(255,255,255,.98), rgba(255,255,255,.9))")
                .set("backdrop-filter", "blur(10px) saturate(1.15)")
                .set("border-bottom", "1px solid var(--lumo-contrast-5pct)")
                .set("box-shadow", "0 3px 12px rgba(15,23,42,.04)")
                .set("padding", "1rem 2rem");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.setMaxWidth("1280px");
        bar.getStyle().set("margin", "0 auto");


        H2 title = new H2("Panel de Reparto: Órdenes por Entregar");
        title.getStyle()
                .set("margin", "0")
                .set("font-weight", "800")
                .set("font-size", "1.75rem")
                .set("letter-spacing", "-0.04em");

        Button refresh = new Button("Actualizar", VaadinIcon.REFRESH.create(), e -> loadData());
        refresh.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        refresh.getStyle().set("font-weight", "600");

        bar.add(title, refresh);
        bar.expand(title);
        band.add(bar);

        return band;
    }

    /* ========================= CHIPS DE FILTRO ========================= */

    private Component buildFilterChips() {
        HorizontalLayout chipsContainer = new HorizontalLayout();
        chipsContainer.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        chipsContainer.setWidthFull();
        chipsContainer.getStyle()
                .set("padding", "16px 0 0 0");

        filterBar.addClassName("orders-filterbar");
        filterBar.getStyle()
                .set("display", "flex")
                .set("gap", "12px");


        filterBar.add(
                chip("LISTO_REPARTO", "Órdenes pendientes", true),
                chip("EN_REPARTO", "Mi reparto activo", false)
        );

        chipsContainer.add(filterBar);
        return chipsContainer;
    }

    private Button chip(String key, String label, boolean initialActive) {

        Button b = new Button(label);
        b.addClassName("chip");
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);

        b.getStyle()
                .set("border-radius", "20px")
                .set("border", "1px solid var(--lumo-contrast-30pct)")
                .set("padding", "8px 16px")
                .set("font-weight", "600")
                .set("transition", "background 0.2s, color 0.2s");

        if (initialActive) {
            b.getStyle()
                    .set("background", "var(--lumo-primary-color)")
                    .set("color", "white")
                    .set("border-color", "var(--lumo-primary-color)");
        }

        b.getElement().setAttribute("data-filter-key", key);


        b.addClickListener(e -> {
            // Actualizar estilo de todos los chips
            filterBar.getChildren().forEach(c -> {
                c.getStyle()
                        .set("background", "transparent")
                        .set("color", "var(--lumo-primary-text-color)")
                        .set("border-color", "var(--lumo-contrast-30pct)");
            });

            // Activar el clicado
            b.getStyle()
                    .set("background", "var(--lumo-primary-color)")
                    .set("color", "white")
                    .set("border-color", "var(--lumo-primary-color)");

            estadoFiltro = PedidoEstado.valueOf(key);
            loadData();
        });

        return b;
    }

    /* ========================= GRID Y WRAPPER ========================= */

    private Component buildWrapper() {
        Div wrap = new Div();
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "20px auto")
                .set("padding", "0 16px 40px 16px")
                .set("width", "100%");

        grid.setWidth("100%");
        grid.setMinHeight("600px");
        styleGrid();

        wrap.add(grid);

        return wrap;
    }

    private void styleGrid() {
        grid.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "18px")
                .set("border", "1px solid var(--lumo-contrast-5pct)")
                .set("box-shadow", "0 18px 45px rgba(15,23,42,.12)")
                .set("overflow", "hidden");
    }

    private void createGrid() {

        // Columna Hora (Corregida la fecha)
        grid.addColumn(p -> p.getFechaRealizacion().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime().format(timeFormatter))
                .setHeader("Hora")
                .setFlexGrow(0)
                .setWidth("80px")
                .setKey("time");

        grid.getColumnByKey("time").setClassNameGenerator(pedido -> "align-right");


        grid.addColumn(p -> p.getCliente().getNombre())
                .setHeader("Cliente")
                .setAutoWidth(true);

        // Columna Dirección (Lógica de Mesa)
        grid.addComponentColumn(pedido -> {
                    String direccion = pedido.getDireccionEnvio();

                    // Si el campo es null o vacío, es que fue una orden en local/mostrador.
                    if (direccion == null || direccion.trim().isEmpty()) {
                        Span span = new Span("ENTREGA A MESA");
                        span.getStyle().set("font-weight", "800").set("color", "var(--lumo-success-color)");
                        return span;
                    }
                    // Si el campo tiene valor, mostramos la dirección de envío.
                    return new Span(direccion);
                }).setHeader("Dirección de entrega")
                .setFlexGrow(2);

        // Columna de Acción (Lógica de Bloqueo)
        grid.addComponentColumn(this::buildActionButton)
                .setHeader("Acción")
                .setFlexGrow(1)
                .setKey("accion");

        // Mensaje de Grid vacío (aunque no use el método setEmptyText)
        Span emptySpan = new Span("No hay pedidos disponibles en este filtro.");
        // grid.setEmptyText(emptySpan); // Comentado por incompatibilidad de versión
    }

    // Lógica de Bloqueo del Repartidor
    private Component buildActionButton(Pedido pedido) {

        // Usa el nuevo método del servicio
        boolean tienePedidoActivo = gestionarPedido.repartidorTienePedidoActivo(repartidor);

        if (pedido.getEstado() == PedidoEstado.LISTO_REPARTO) {

            Button tomarPedido = new Button("Tomar Pedido", VaadinIcon.TRUCK.create());
            tomarPedido.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            tomarPedido.getStyle().set("border-radius", "10px").set("font-weight", "700");

            // ⭐ Requisito: Bloquear si ya tiene un pedido activo
            if (tienePedidoActivo) {
                tomarPedido.setEnabled(false);
                tomarPedido.setText("Reparto en curso");
                tomarPedido.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                tomarPedido.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_CONTRAST);
                tomarPedido.getStyle().set("opacity", "0.7");
                return tomarPedido;
            }

            tomarPedido.addClickListener(e -> {
                // 1. Asignar el repartidor (REQUIERE EL CAMPO EN LA ENTIDAD PEDIDO)
                pedido.setRepartidor(repartidor);
                // 2. Cambiar estado
                pedido.setEstado(PedidoEstado.EN_REPARTO);
                gestionarPedido.save(pedido);

                Notification.show("Pedido #" + pedido.getId() + " tomado y en reparto.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_PRIMARY);
                loadData();
            });

            return tomarPedido;
        }

        if (pedido.getEstado() == PedidoEstado.EN_REPARTO) {

            Button entregado = new Button("Marcar Entregado", VaadinIcon.CHECK_CIRCLE.create());
            entregado.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
            entregado.getStyle().set("border-radius", "10px").set("font-weight", "700");

            entregado.addClickListener(e -> {
                pedido.setEstado(PedidoEstado.ENTREGADO);
                // Si el pedido está marcado como entregado, el repartidor queda libre
                gestionarPedido.save(pedido);

                Notification.show(" Pedido #" + pedido.getId() + " Entregado con éxito.", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadData();
            });

            return entregado;
        }

        return new Span("—");
    }

    /* ========================= DATA ========================= */

    private void loadData() {
        Set<Pedido> pedidos;

        if (estadoFiltro == PedidoEstado.LISTO_REPARTO) {
            // Cargar todos los pedidos listos
            pedidos = gestionarPedido.pedidos_por_estado(PedidoEstado.LISTO_REPARTO);

        } else if (estadoFiltro == PedidoEstado.EN_REPARTO) {
            // Cargar el/los pedidos que tiene asignados este repartidor
            pedidos = gestionarPedido.pedidos_por_repartidor_y_estado(repartidor, PedidoEstado.EN_REPARTO);

        } else {
            pedidos = Set.of();
        }

        grid.setItems(pedidos);
    }
}