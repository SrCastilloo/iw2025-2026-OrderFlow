package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.QueryParameters;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.business.services.ClienteSesionService;
import es.uca.orderflow.business.services.GestionarPedido;
import es.uca.orderflow.business.services.PaymentMethod;
import es.uca.orderflow.business.services.PedidoEstado;
import es.uca.orderflow.persistence.data.Detalle_PedidoRepository;
import es.uca.orderflow.persistence.data.PedidoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Mis pedidos")
@Route("/cliente/pedidos")
@AnonymousAllowed
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@CssImport("./styles/pedidos.css")
public class PedidosClienteView extends VerticalLayout {

    private final PedidoRepository pedidoRepository;
    private final Detalle_PedidoRepository detallePedidoRepository;
    private final ClienteSesionService clienteSesionService;
    private final I18NProvider i18nProvider;
    private final GestionarPedido gestionarPedido;

    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es","ES"));
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final Div grid = new Div();
    private final Div filterBar = new Div();

    private List<Pedido> pedidos = List.of();
    private String estadoFiltro = "TODOS";

    @PostConstruct
    void init() {
        setPageTitle(getTranslation("view.orders.title"));

        setId("orders-root");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        getStyle().set("background", "var(--lumo-contrast-5pct)");

        add(buildTopBar(), buildFilters(), buildGrid());
        loadAndRender();
    }

    private Component buildTopBar() {
        Div band = new Div();
        band.addClassName("orders-topbar");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getStyle().set("max-width", "1280px").set("margin", "0 auto").set("padding", "0 16px");

        H2 title = new H2(getTranslation("view.orders.title"));
        title.getStyle().set("margin","0").set("font-weight","900").set("font-size", "2rem");

        Button back = new Button(getTranslation("button.back"), VaadinIcon.ARROW_LEFT.create());
        // Se mantiene el estilo para el botón de retroceso.
        back.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        back.addClickListener(e -> UI.getCurrent().navigate("/cliente"));

        bar.add(title);
        bar.expand(title);
        bar.add(back);

        band.add(bar);
        return band;
    }

    private Component buildFilters() {
        Div wrap = new Div();
        wrap.getStyle().set("max-width", "1280px").set("margin", "10px auto").set("padding", "0 16px");

        filterBar.addClassName("orders-filterbar");
        filterBar.add(
                filterChip("TODOS", true),
                // ⭐ CAMBIO 1: Añadir el filtro para el nuevo estado PENDIENTE
                filterChip("PENDIENTE", false),
                filterChip("PREPARACION", false),
                filterChip("LISTO_REPARTO", false),
                filterChip("EN_REPARTO", false),
                filterChip("ENTREGADO", false)
        );
        wrap.add(filterBar);
        return wrap;
    }

    private Button filterChip(String key, boolean active) {
        String label = getTranslatedStatus(key);

        Button b = new Button(label);
        b.addThemeVariants(ButtonVariant.LUMO_SMALL);
        b.addClassName("chip");

        if (key.equals(estadoFiltro) || active && estadoFiltro.equals("TODOS")) {
            b.addClassName("active");
            b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        } else {
            b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }

        b.addClickListener(e -> {
            estadoFiltro = key;
            filterBar.getChildren().forEach(c -> {
                c.getElement().getClassList().remove("active");
                if (c instanceof Button cb) {
                    cb.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
                    cb.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
                }
            });
            b.addClassName("active");
            b.removeThemeVariants(ButtonVariant.LUMO_TERTIARY);
            b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            render();
        });
        return b;
    }

    private Component buildGrid() {
        Div wrap = new Div();
        wrap.getStyle().set("max-width", "1280px").set("margin", "0 auto").set("padding", "10px 16px 30px");
        grid.addClassName("orders-grid");
        wrap.add(grid);
        return wrap;
    }

    private void loadAndRender() {
        var cli = clienteSesionService.getActual();
        if (cli == null) {
            Div empty = new Div(new H3(getTranslation("orders.error.not_logged_in")));
            empty.addClassName("orders-empty");
            grid.removeAll();
            grid.add(empty);
            return;
        }
        var set = pedidoRepository.findByCliente(cli);
        pedidos = set == null ? List.of() : set.stream()
                .sorted(Comparator.comparing(Pedido::getFechaRealizacion, Comparator.nullsLast(Comparator.naturalOrder())).reversed())
                .collect(Collectors.toList());
        render();
    }

    private void render() {
        grid.removeAll();
        if (pedidos.isEmpty()) {
            Div empty = new Div(new H4("🤔 " + getTranslation("orders.empty.title")),
                    new Paragraph(getTranslation("orders.empty.subtitle")));
            empty.addClassName("orders-empty");
            grid.add(empty);
            return;
        }
        pedidos.stream()
                .filter(p -> "TODOS".equals(estadoFiltro) || (p.getEstado()!=null && p.getEstado().name().equals(estadoFiltro)))
                .forEach(p -> grid.add(orderCard(p)));
    }

    private Component orderCard(Pedido p) {
        Div card = new Div();
        card.addClassName("order-card");

        // Cabecera (ID y Fecha)
        VerticalLayout idAndDate = new VerticalLayout();
        idAndDate.setPadding(false);
        idAndDate.setSpacing(false);

        H4 code = new H4("#" + p.getId());
        code.getStyle().set("margin", "0").set("font-weight", "900").set("font-size", "1.2rem");

        Span fecha = new Span(p.getFechaRealizacion()==null?"-":fmt.format(p.getFechaRealizacion()));
        fecha.addClassNames("muted", "order-date");

        idAndDate.add(code, fecha);

        // Badge de estado
        Span estado = estadoBadge(p);

        HorizontalLayout head = new HorizontalLayout();
        head.setWidthFull();
        head.setAlignItems(FlexComponent.Alignment.CENTER);
        head.addClassName("order-card-head");
        head.add(idAndDate);
        head.expand(idAndDate);
        head.add(estado);


        // Progreso (Progress Strip)
        Div progress = progressStrip(p);

        // Meta (Método y Transacción)
        Div meta = new Div();
        meta.addClassName("order-meta");
        meta.add(
                metaRow(getTranslation("orders.meta.method"), p.getPaymentMethod()==null?"-":getTranslatedPayment(p.getPaymentMethod())),
                metaRow(getTranslation("orders.meta.transaction"), p.getPaymentTxnId()==null?"-":p.getPaymentTxnId())
        );


        // Líneas de Pedido (Detalle)
        UnorderedList ul = new UnorderedList();
        ul.addClassName("order-items");
        var detalles = detallePedidoRepository.findByPedido(p);
        BigDecimal total = BigDecimal.ZERO;

        for (var d : detalles) {
            BigDecimal imp = d.getImporte()==null?BigDecimal.ZERO:d.getImporte();
            total = total.add(imp);

            Span itemQty = new Span(d.getCantidad() + " x ");
            itemQty.addClassName("item-qty");
            Span itemName = new Span(d.getProducto()==null?"-":d.getProducto().getNombre());
            Span itemPrice = new Span(euro.format(imp));
            itemPrice.getStyle().set("margin-left", "auto").set("font-weight", "600");

            HorizontalLayout itemRow = new HorizontalLayout(itemQty, itemName, itemPrice);
            itemRow.setWidthFull();
            itemRow.setSpacing(true);
            itemRow.setAlignItems(FlexComponent.Alignment.CENTER);
            itemRow.addClassName("item-row");

            ul.add(new ListItem(itemRow));
        }

        // Total (Destacado)
        HorizontalLayout totalRow = new HorizontalLayout();
        totalRow.setWidthFull();
        totalRow.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        totalRow.addClassName("order-total");

        Span totalLabel = new Span(getTranslation("orders.total"));
        totalLabel.getStyle().set("font-weight", "900").set("font-size", "1.3rem");
        Span totalValue = new Span(euro.format(total));
        totalValue.getStyle().set("font-weight", "900").set("font-size", "1.3rem").set("color", "var(--lumo-primary-color)");

        totalRow.add(totalLabel, totalValue);

        // Footer (Botones de Acción)
        HorizontalLayout foot = new HorizontalLayout();
        foot.setWidthFull();
        foot.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        foot.addClassName("order-card-foot");
        foot.setSpacing(true);


        // ====================================================================
        // ⭐ LÓGICA DE CANCELACIÓN Y MODIFICACIÓN CONDICIONAL
        // ====================================================================

        // ⭐ CAMBIO 2: Mostrar botones de acción solo si el estado es PENDIENTE
        if (p.getEstado() == PedidoEstado.PENDIENTE) {
            // 1. Botón de Cancelar
            Button cancelBtn = new Button(getTranslation("button.cancel_order"), VaadinIcon.CLOSE_CIRCLE.create());
            cancelBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
            cancelBtn.addClickListener(e -> showCancelDialog(p));
            foot.add(cancelBtn);

            // 2. Botón de Modificar (Asegurando la llamada al servicio y la visibilidad)
            Button modifyBtn = new Button(getTranslation("button.modify_order"), VaadinIcon.PENCIL.create());
            modifyBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            modifyBtn.addClickListener(e -> onModificarPedido(p.getId()));

            foot.add(modifyBtn);
        }

        // Botón de Factura (Siempre visible, independientemente del estado)
        Button facturaBtn = new Button(getTranslation("button.download_invoice"), VaadinIcon.FILE_TEXT.create());
        facturaBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Anchor factura = new Anchor("/api/pedidos/" + p.getId() + "/factura.pdf", facturaBtn);
        factura.setTarget("_blank");

        foot.add(factura);

        card.add(head, progress, meta, ul, totalRow, foot);
        return card;
    }

    private Component metaRow(String label, String value) {
        Span l = new Span(label); l.addClassName("meta-l");
        Span v = new Span(value); v.addClassName("meta-v");
        return new Div(l, v) {{ addClassName("meta-row"); }};
    }

    private String getTranslatedPayment(PaymentMethod pm) {
        return getTranslation("payment." + pm.name().toLowerCase());
    }

    private Span estadoBadge(Pedido p) {
        String txt = p.getEstado()==null?"-":getTranslatedStatus(p.getEstado().name());
        Span b = new Span(txt);
        b.addClassName("estado-badge");
        b.getStyle().set("padding", "4px 10px").set("border-radius", "8px").set("font-weight", "700");

        if (p.getEstado()==null) return b;
        switch (p.getEstado().name()) {
            // ⭐ CAMBIO 3: Añadir el estilo para PENDIENTE
            case "PENDIENTE" -> b.getStyle().set("background","#f0f9ff").set("color","#0c4a6e"); // Tono azul/gris claro
            case "PREPARACION" -> b.getStyle().set("background","#fff7ed").set("color","#9a3412");
            case "LISTO_REPARTO" -> b.getStyle().set("background","#eff6ff").set("color","#1d4ed8");
            case "EN_REPARTO" -> b.getStyle().set("background","#f5f3ff").set("color","#6d28d9");
            case "ENTREGADO" -> b.getStyle().set("background","#ecfdf5").set("color","#065f46");
            case "CANCELADO" -> b.getStyle().set("background","#fef2f2").set("color","#b91c1c"); // Opcional: Para CANCELADO, si lo filtra
        }
        return b;
    }

    private Div progressStrip(Pedido p) {
        Div wrap = new Div(); wrap.addClassName("progress-wrap");
        // ⭐ CAMBIO 4: Incluir PENDIENTE en la tira de progreso
        String[] steps = {"PENDIENTE", "PREPARACION","LISTO_REPARTO","EN_REPARTO","ENTREGADO"};
        int idx = p.getEstado()==null ? -1 : Arrays.asList(steps).indexOf(p.getEstado().name());

        for (int i = 0; i < steps.length; i++){
            Div s = new Div(); s.addClassName("progress-step");
            if (i <= idx) s.addClassName("done");

            Span icon = new Span(); icon.addClassName("step-icon");
            Span label = new Span(getTranslatedStatus(steps[i]));
            label.addClassName("step-label");

            s.add(icon, label);
            wrap.add(s);
        }
        return wrap;
    }

    /** Muestra un diálogo de confirmación para cancelar el pedido. */
    private void showCancelDialog(Pedido p) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(getTranslation("dialog.cancel.title", p.getId()));

        dialog.add(new Paragraph(getTranslation("dialog.cancel.confirm_msg")));

        Button confirmBtn = new Button(getTranslation("button.confirm_cancel"), e -> {
            try {
                // ⭐ LLAMADA AL SERVICIO DE NEGOCIO (la lógica de validación PENDIENTE está en el servicio)
                gestionarPedido.cancelarPedido(p.getId());

                Notification.show(getTranslation("notification.order_cancelled_success", p.getId()), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

                dialog.close();

                // Recargar la lista de pedidos para reflejar el cambio de estado (a CANCELADO)
                loadAndRender();
            } catch (IllegalStateException ex) {
                // Captura si el estado ya no es PENDIENTE
                Notification.show(ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                dialog.close();
                // Recargar para mostrar el estado actual
                loadAndRender();
            } catch (Exception ex) {
                // Captura errores genéricos o NoSuchElementException
                Notification.show(getTranslation("notification.error.cancel") + ": " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                dialog.close();
            }
        });
        confirmBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelBtn = new Button(getTranslation("button.dismiss"), e -> dialog.close());
        cancelBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelBtn, confirmBtn);
        dialog.open();
    }


    private String getTranslatedStatus(String statusKey) {
        if ("TODOS".equals(statusKey)) {
            return getTranslation("filter.all");
        }
        String translationKey = "status." + statusKey.toLowerCase();
        return getTranslation(translationKey);
    }

    private void setPageTitle(String title) {
        UI.getCurrent().getPage().setTitle(title);
    }
    private void onModificarPedido(Long pedidoId) {
        try {
            Cliente cliente = clienteSesionService.getActual();
            if (cliente == null) {
                Notification.show(getTranslation("notification.error.not_logged_in"),
                                3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
                return;
            }

            // 1. Cargar el pedido en el carrito
            gestionarPedido.cargarPedidoEnCarrito(pedidoId, cliente);

            // 2. Guardar en sesión qué pedido estamos modificando
            clienteSesionService.setPedidoEnModificacionId(pedidoId);

            // 3. Navegar al carrito (ya no hace falta el parámetro "modifying")
            UI.getCurrent().navigate("/cliente/carrito");

        } catch (Exception e) {
            Notification.show(
                            getTranslation("notification.error.modify") + ": " + e.getMessage(),
                            4000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

}