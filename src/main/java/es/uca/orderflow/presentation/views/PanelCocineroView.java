package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.business.services.GestionarPedido;
import es.uca.orderflow.business.services.PedidoEstado;
import es.uca.orderflow.persistence.data.Detalle_PedidoRepository;
import es.uca.orderflow.persistence.data.PedidoRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Locale;
import java.util.Set;

@PageTitle("Panel Cocinero")
@Route("/backoffice/cocinero")
@AnonymousAllowed //de momento
public class PanelCocineroView extends VerticalLayout {

    private final PedidoRepository pedidoRepository;
    private final GestionarPedido gestionarPedido;
    private final Detalle_PedidoRepository detallePedidoRepository;

    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es","ES"));
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    public PanelCocineroView(PedidoRepository pedidoRepository,
                             GestionarPedido gestionarPedido,
                             Detalle_PedidoRepository detallePedidoRepository) {

        this.pedidoRepository = pedidoRepository;
        this.gestionarPedido = gestionarPedido;
        this.detallePedidoRepository = detallePedidoRepository;

        /* ======== ESTILO GENERAL DEL ROOT ======== */
        setId("cook-root");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setHeightFull(); // Aseguramos que el layout ocupa toda la altura

        // Fondo degradado sutil
        getStyle().set("background",
                "radial-gradient(1200px 600px at 50% -200px, rgba(255,255,255,.75), rgba(255,255,255,0))," +
                        "linear-gradient(180deg,#fefefe 0%, #f7f7f7 40%, #ffffff 100%)");

        createGrid();

        add(buildTopBar(), buildWrapper());
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
        bar.getStyle().set("margin", "0 auto"); // Centrar el contenido de la barra

        // ⭐ CAMBIO: Actualizar título para incluir Pendientes
        H2 title = new H2("Panel de Cocina: Órdenes Pendientes y en Preparación");
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

    /* ========================= CONTENEDOR PRIMARIO ========================= */

    private Component buildWrapper() {
        Div wrap = new Div();
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "20px auto")
                .set("padding", "0 16px 40px 16px") // Más padding abajo
                .set("width", "100%");

        // Estilos para el Grid. Quitamos el height fijo para que sea flexible.
        grid.setWidth("100%");
        grid.setMinHeight("600px"); // Altura mínima

        styleGrid();

        wrap.add(grid);
        return wrap;
    }

    private void styleGrid() {
        grid.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "18px") // Bordes más grandes
                .set("border", "1px solid var(--lumo-contrast-5pct)") // Borde más sutil
                .set("box-shadow", "0 18px 45px rgba(15,23,42,.12)") // Sombra más prominente
                .set("overflow", "hidden"); // Asegura que las esquinas se corten
    }

    /* ========================= GRID ========================= */

    private void createGrid() {
        grid.addColumn(pedido -> "Mesa/Cliente: " + pedido.getCliente().getNombre())
                .setHeader("ORDEN")
                .setFlexGrow(2)
                .setResizable(true);

        // Columna de hora
        grid.addColumn(pedido -> {
                    Date date = pedido.getFechaRealizacion();
                    if (date == null) return "N/A";

                    // ⭐ CORRECCIÓN: Convertir java.util.Date a LocalDateTime para usar DateTimeFormatter
                    LocalDateTime localDateTime = date.toInstant()
                            .atZone(ZoneId.systemDefault())
                            .toLocalDateTime();

                    return localDateTime.format(timeFormatter);
                })
                .setHeader("HORA")
                .setFlexGrow(0)
                .setWidth("80px")
                .setKey("time");

        // Columna de Estado con estilo visual
        grid.addComponentColumn(this::buildEstadoTag)
                .setHeader("ESTADO")
                .setFlexGrow(1)
                .setKey("estado");

        // Ver productos (Botón con ícono)
        grid.addComponentColumn(pedido -> {
                    Button ver = new Button("Ver Productos", VaadinIcon.EXTERNAL_LINK.create());
                    ver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                    ver.getStyle()
                            .set("border-radius", "8px")
                            .set("padding", "6px 12px")
                            .set("font-weight", "600")
                            .set("transition", "background .15s ease");
                    ver.addClickListener(e -> openProductosDialog(pedido));
                    return ver;
                }).setHeader("DETALLES")
                .setFlexGrow(1)
                .setKey("detalles");

        // ⭐ CAMBIO: Usar helper para generar el botón de acción condicional
        grid.addComponentColumn(this::buildAccionButton)
                .setHeader("ACCIÓN")
                .setFlexGrow(1)
                .setKey("accion");

        // Estilos específicos para las celdas
        // Corrección de ColumnTextAlign: Usamos setClassNameGenerator y CSS externo (Clase 'align-right')
        grid.getColumnByKey("time").setClassNameGenerator(pedido -> "align-right");

        // Mensaje cuando no hay pedidos
        Span emptySpan = new Span("🎉 ¡Genial! No hay pedidos en preparación. Tómate un respiro.");
        emptySpan.getStyle().set("font-style", "italic").set("color", "var(--lumo-secondary-text-color)");
    }

    // ⭐ NUEVO MÉTODO: Genera el botón de acción según el estado
    private Component buildAccionButton(Pedido pedido) {
        if (pedido.getEstado() == PedidoEstado.PENDIENTE) {
            Button prepararBtn = new Button("Poner en PREPARACIÓN", VaadinIcon.TIMER.create());
            prepararBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_CONTRAST);
            prepararBtn.getStyle()
                    .set("border-radius", "10px")
                    .set("font-weight", "700");
            prepararBtn.addClickListener(e -> cambiarEstado(pedido));
            return prepararBtn;

        } else if (pedido.getEstado() == PedidoEstado.PREPARACION) {
            Button listoBtn = new Button("Marcar como LISTO", VaadinIcon.CHECK_CIRCLE.create());
            listoBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
            listoBtn.getStyle()
                    .set("border-radius", "10px")
                    .set("font-weight", "700");
            listoBtn.addClickListener(e -> cambiarEstado(pedido));
            return listoBtn;
        }

        // Para otros estados, no mostramos botón de acción
        return new Span("-");
    }

    // Helper para el tag de estado visual
    private Component buildEstadoTag(Pedido pedido) {
        Span tag = new Span(pedido.getEstado().toString());
        tag.getStyle()
                .set("display", "inline-block")
                .set("padding", "4px 10px")
                .set("border-radius", "999px")
                .set("font-weight", "700")
                .set("font-size", "var(--lumo-font-size-s)");

        // Estilos basados en el estado
        if (pedido.getEstado() == PedidoEstado.PENDIENTE) { // ⭐ NUEVO ESTILO PARA PENDIENTE
            tag.getStyle()
                    .set("background", "#e0f2f1") // Cyan claro
                    .set("color", "#164e63");   // Cyan oscuro
        } else if (pedido.getEstado() == PedidoEstado.PREPARACION) {
            tag.getStyle()
                    .set("background", "#ffedd5") // Naranja muy claro
                    .set("color", "#9a3412");   // Naranja oscuro
        } else if (pedido.getEstado() == PedidoEstado.LISTO_REPARTO) {
            tag.getStyle()
                    .set("background", "#d1fae5") // Verde muy claro
                    .set("color", "#065f46");   // Verde oscuro
        } else {
            // Estado por defecto o no esperado
            tag.getStyle()
                    .set("background", "var(--lumo-contrast-5pct)")
                    .set("color", "var(--lumo-secondary-text-color)");
        }
        return tag;
    }

    /* ========================= LÓGICA DE DATOS ========================= */

    private void loadData() {
        // ⭐ CAMBIO: Cargar pedidos en PENDIENTE y PREPARACION
        Set<Pedido> pendientes = pedidoRepository.findByEstado(PedidoEstado.PENDIENTE);
        Set<Pedido> preparacion = pedidoRepository.findByEstado(PedidoEstado.PREPARACION);

        // Combinar los sets. Si necesitas un orden específico (e.g., PENDIENTE primero),
        // deberás usar un Comparator y convertir a List.
        pendientes.addAll(preparacion);

        grid.setItems(pendientes);
    }

    private void cambiarEstado(Pedido pedido) {
        String notificationMessage = "";
        PedidoEstado nuevoEstado;

        // ⭐ CAMBIO: Lógica de transición de estados
        if (pedido.getEstado() == PedidoEstado.PENDIENTE) {
            nuevoEstado = PedidoEstado.PREPARACION;
            notificationMessage = "Pedido #" + pedido.getId() + " marcado como EN PREPARACIÓN.";
        } else if (pedido.getEstado() == PedidoEstado.PREPARACION) {
            nuevoEstado = PedidoEstado.LISTO_REPARTO;
            notificationMessage = "Pedido #" + pedido.getId() + " marcado como LISTO.";
        } else {
            Notification.show(
                    "El pedido #" + pedido.getId() + " tiene un estado no modificable (" + pedido.getEstado() + ").",
                    3000,
                    Notification.Position.TOP_CENTER
            ).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        pedido.setEstado(nuevoEstado);
        gestionarPedido.save(pedido);

        // Recargar la data para que el pedido se actualice o desaparezca del grid
        loadData();

        Notification.show(
                notificationMessage,
                3000,
                Notification.Position.TOP_CENTER
        ).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    /* ========================= DIALOG DE PRODUCTOS (Sin cambios) ========================= */

    private void openProductosDialog(Pedido p) {
        Dialog dlg = new Dialog();
        dlg.setWidth("500px");
        dlg.getElement().getThemeList().add("no-padding");

        dlg.setHeaderTitle("Detalle de Pedido #" + p.getId());

        /* ======== LISTA DE PRODUCTOS ======== */
        VerticalLayout listContent = new VerticalLayout();
        listContent.setSpacing(false);
        listContent.setPadding(false);

        var detalles = detallePedidoRepository.findByPedido(p);
        BigDecimal total = BigDecimal.ZERO;

        for (var d : detalles) {
            BigDecimal importe = d.getImporte() == null ? BigDecimal.ZERO : d.getImporte();
            total = total.add(importe);

            HorizontalLayout itemRow = new HorizontalLayout();
            itemRow.setWidthFull();
            itemRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            Span nameQty = new Span(d.getCantidad() + " × " +
                    (d.getProducto() == null ? "Producto Desconocido" : d.getProducto().getNombre()));
            nameQty.getStyle().set("font-weight", "600");

            Span price = new Span(euro.format(importe));
            price.getStyle().set("font-weight", "500").set("color", "var(--lumo-secondary-text-color)");

            itemRow.add(nameQty, price);
            itemRow.expand(nameQty);

            // Separador visual
            Div separator = new Div();
            separator.setWidthFull();
            separator.setHeight("1px");
            separator.getStyle().set("background", "var(--lumo-contrast-5pct)").set("margin", "6px 0");

            listContent.add(itemRow, separator);
        }

        // Remover el último separador si hay elementos
        if (listContent.getComponentCount() > 0) {
            listContent.remove(listContent.getComponentAt(listContent.getComponentCount() - 1));
        }


        /* ======== TOTAL ======== */
        HorizontalLayout totalRow = new HorizontalLayout();
        totalRow.setWidthFull();
        totalRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        totalRow.getStyle()
                .set("padding-top", "12px")
                .set("border-top", "2px solid var(--lumo-contrast-10pct)");

        Span totalLabel = new Span("TOTAL PEDIDO");
        totalLabel.getStyle().set("font-weight", "bold").set("font-size", "1.1rem");

        Span totalValue = new Span(euro.format(total));
        totalValue.getStyle().set("font-weight", "800").set("font-size", "1.3rem").set("color", "var(--lumo-primary-text-color)");

        totalRow.add(totalLabel, totalValue);

        // Contenedor principal del contenido del diálogo
        VerticalLayout content = new VerticalLayout(listContent, totalRow);
        content.setPadding(true);
        content.getStyle().set("padding", "20px");

        dlg.add(content);

        /* ======== FOOTER ======== */
        Button cerrar = new Button("Cerrar", VaadinIcon.CLOSE_SMALL.create(), e -> dlg.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dlg.getFooter().add(cerrar);
        dlg.open();
    }
}