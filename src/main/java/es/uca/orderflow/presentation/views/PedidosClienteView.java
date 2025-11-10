// src/main/java/es/uca/orderflow/presentation/views/PedidosClienteView.java
package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.component.dependency.CssImport;     // <-- importa CssImport

import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.business.services.ClienteSesionService;
import es.uca.orderflow.business.services.PaymentMethod;
import es.uca.orderflow.persistence.data.Detalle_PedidoRepository;
import es.uca.orderflow.persistence.data.PedidoRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Mis pedidos")
@Route("/cliente/pedidos")
@AnonymousAllowed
@RequiredArgsConstructor
@CssImport("./styles/pedidos.css")   // <-- aplica el CSS externo
public class PedidosClienteView extends VerticalLayout {

    private final PedidoRepository pedidoRepository;
    private final Detalle_PedidoRepository detallePedidoRepository;
    private final ClienteSesionService clienteSesionService;

    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es","ES"));
    private final SimpleDateFormat fmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final Div grid = new Div();
    private final Div filterBar = new Div();

    private List<Pedido> pedidos = List.of();
    private String estadoFiltro = "TODOS";

    @PostConstruct
    void init() {
        setId("orders-root");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        add(buildTopBar(), buildFilters(), buildGrid());
        loadAndRender();
    }

    private Component buildTopBar() {
        Div band = new Div();
        band.addClassName("orders-topbar");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);

        H2 title = new H2("Mis pedidos");
        title.getStyle().set("margin","0").set("font-weight","900");

        Button back = new Button("Volver", VaadinIcon.ARROW_LEFT.create());
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.addClickListener(e -> UI.getCurrent().navigate("/cliente"));

        bar.add(title);
        bar.expand(title);
        bar.add(back);

        band.add(bar);
        return band;
    }

    private Component buildFilters() {
        filterBar.addClassName("orders-filterbar");
        filterBar.add(
                filterChip("TODOS", true),
                filterChip("PREPARACION", false),
                filterChip("LISTO_REPARTO", false),
                filterChip("EN_REPARTO", false),
                filterChip("ENTREGADO", false)
        );
        return filterBar;
    }

    private Button filterChip(String key, boolean active) {
        String label = key.equals("TODOS") ? "Todos" : key.replace('_',' ');
        Button b = new Button(label);
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        b.addClassName("chip");
        if (active) b.addClassName("active");
        b.addClickListener(e -> {
            estadoFiltro = key;
            filterBar.getChildren().forEach(c -> c.getElement().getClassList().remove("active"));
            b.addClassName("active");
            render();
        });
        return b;
    }

    private Component buildGrid() {
        grid.addClassName("orders-grid");
        return grid;
    }

    private void loadAndRender() {
        var cli = clienteSesionService.getActual();
        if (cli == null) {
            Div empty = new Div(new H3("Debes iniciar sesión para ver tus pedidos."));
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
            Div empty = new Div(new H4("Aún no tienes pedidos"),
                    new Paragraph("Cuando compres algo, lo verás aquí con su estado y factura."));
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

        // Cabecera
        HorizontalLayout head = new HorizontalLayout();
        head.setWidthFull();
        head.setAlignItems(FlexComponent.Alignment.CENTER);

        H4 code = new H4("#" + p.getId());
        Span fecha = new Span(p.getFechaRealizacion()==null?"-":fmt.format(p.getFechaRealizacion()));
        fecha.addClassName("muted");
        Span estado = estadoBadge(p);

        Button facturaBtn = new Button("Descargar factura", VaadinIcon.FILE_TEXT.create());
        facturaBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Anchor factura = new Anchor("/api/pedidos/" + p.getId() + "/factura.pdf", facturaBtn);
        factura.setTarget("_blank");

        head.add(code, fecha, estado);
        head.expand(code);
        head.add(factura);

        // Meta
        Div meta = new Div();
        meta.addClassName("order-meta");
        meta.add(
                metaRow("Método", p.getPaymentMethod()==null?"-":prettyPayment(p.getPaymentMethod())),
                metaRow("Transacción", p.getPaymentTxnId()==null?"-":p.getPaymentTxnId())
        );

        // Progreso
        Div progress = progressStrip(p);

        // Líneas
        UnorderedList ul = new UnorderedList();
        var detalles = detallePedidoRepository.findByPedido(p);
        double total = 0d;
        for (var d : detalles) {
            double imp = d.getImporte()==null?0d:d.getImporte().doubleValue();
            total += imp;
            ul.add(new ListItem(d.getCantidad() + " × " + (d.getProducto()==null?"-":d.getProducto().getNombre())
                    + " — " + euro.format(imp)));
        }

        Paragraph tot = new Paragraph("Total: " + euro.format(total));
        tot.getStyle().set("fontWeight","900").set("margin","10px 0 0 0");

        // Footer (si quieres más acciones en el futuro)
        HorizontalLayout foot = new HorizontalLayout();
        foot.setWidthFull();
        foot.setJustifyContentMode(JustifyContentMode.END);
        Button detalle = new Button("Ver detalle", VaadinIcon.CLIPBOARD_TEXT.create());
        detalle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        foot.add(detalle);

        card.add(head, meta, progress, ul, tot, foot);
        return card;
    }

    private Component metaRow(String label, String value) {
        Span l = new Span(label); l.addClassName("meta-l");
        Span v = new Span(value); v.addClassName("meta-v");
        return new Div(l, v) {{ addClassName("meta-row"); }};
    }

    private String prettyPayment(PaymentMethod pm) {
        return switch (pm) {
            case TARJETA -> "Tarjeta";
            case PAYPAL -> "PayPal";
            case BIZUM -> "Bizum";
            case TRANSFERENCIA -> "Transferencia";
        };
    }

    private Span estadoBadge(Pedido p) {
        String txt = p.getEstado()==null?"-":p.getEstado().name().replace('_',' ');
        Span b = new Span(txt);
        b.addClassName("estado");
        if (p.getEstado()==null) return b;
        switch (p.getEstado()) {
            case PREPARACION -> b.getStyle().set("background","#fff7ed").set("color","#9a3412").set("borderColor","#fed7aa");
            case LISTO_REPARTO -> b.getStyle().set("background","#eff6ff").set("color","#1d4ed8").set("borderColor","#bfdbfe");
            case EN_REPARTO -> b.getStyle().set("background","#f5f3ff").set("color","#6d28d9").set("borderColor","#ddd6fe");
            case ENTREGADO -> b.getStyle().set("background","#ecfdf5").set("color","#065f46").set("borderColor","#bbf7d0");
        }
        return b;
    }

    private Div progressStrip(Pedido p) {
        Div wrap = new Div(); wrap.addClassName("progress-wrap");
        String[] steps = {"PREPARACION","LISTO_REPARTO","EN_REPARTO","ENTREGADO"};
        int idx = p.getEstado()==null ? -1 : Arrays.asList(steps).indexOf(p.getEstado().name());
        for (int i=0;i<steps.length;i++){
            Div s = new Div(); s.addClassName("progress-step");
            s.add(new Span(steps[i].replace('_',' ')));
            if (i<=idx) s.addClassName("done");
            wrap.add(s);
            if (i<steps.length-1){
                Div sep = new Div(); sep.addClassName("progress-sep");
                if (i<idx) sep.addClassName("done");
                wrap.add(sep);
            }
        }
        return wrap;
    }
}
