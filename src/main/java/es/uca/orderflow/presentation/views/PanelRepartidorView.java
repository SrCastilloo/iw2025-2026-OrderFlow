package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.business.services.GestionarPedido;
import es.uca.orderflow.business.services.PedidoEstado;

import java.util.Set;

@PageTitle("Panel Repartidor")
@Route("/backoffice/repartidor")
@AnonymousAllowed
@CssImport("./styles/empleados.css")
public class PanelRepartidorView extends VerticalLayout {

    private final GestionarPedido gestionarPedido;
    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    private final Div filterBar = new Div();
    private PedidoEstado estadoFiltro = PedidoEstado.LISTO_REPARTO;

    public PanelRepartidorView(GestionarPedido gestionarPedido) {
        this.gestionarPedido = gestionarPedido;

        setId("cook-root");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        getStyle().set("background",
                "radial-gradient(1200px 600px at 50% -200px, rgba(255,255,255,.75), rgba(255,255,255,0))," +
                        "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)");

        createGrid();

        add(buildTopBar(), buildFilterChips(), buildWrapper());

        loadData();
    }

    /* ========================= TOP BAR ========================= */

    private Component buildTopBar() {
        Div band = new Div();
        band.setWidthFull();
        band.setId("cook-band");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);

        H2 title = new H2("Panel del Repartidor");
        title.getStyle().set("margin", "0").set("font-weight", "900");

        bar.add(title);
        band.add(bar);
        return band;
    }

    /* ========================= CHIPS DE FILTRO ========================= */

    private Component buildFilterChips() {
        filterBar.addClassName("orders-filterbar");
        filterBar.getStyle()
                .set("display", "flex")
                .set("justify-content", "center")
                .set("gap", "12px")
                .set("padding", "16px 0");


        filterBar.add(
                chip("LISTO_REPARTO", "Listo reparto", true),
                chip("EN_REPARTO", "En reparto", false)
        );

        return filterBar;
    }

    private Button chip(String key, String label, boolean active) {

        Button b = new Button(label);
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        b.addClassName("chip");

        b.getStyle()
                .set("border-radius", "20px")
                .set("border", "1px solid #d0d0d0")
                .set("padding", "6px 14px")
                .set("font-weight", "600");

        if (active) {
            b.getStyle()
                    .set("background", "#2563eb")
                    .set("color", "white")
                    .set("border-color", "#1e40af");
        }

        b.addClickListener(e -> {
            // quitar activo a todos
            filterBar.getChildren().forEach(c -> {
                c.getElement().getStyle().remove("background");
                c.getElement().getStyle().remove("color");
                c.getElement().getStyle().set("border", "1px solid #d0d0d0");
            });

            // activar el clicado
            b.getStyle()
                    .set("background", "#2563eb")
                    .set("color", "white")
                    .set("border-color", "#1e40af");

            estadoFiltro = PedidoEstado.valueOf(key);
            loadData();
        });

        return b;
    }

    /* ========================= GRID ========================= */

    private Component buildWrapper() {
        Div wrap = new Div();
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "20px auto")
                .set("padding", "0 16px")
                .set("width", "100%");

        grid.setWidth("100%");
        grid.setHeight("600px");
        styleGrid();

        wrap.add(grid);

        return wrap;
    }

    private void styleGrid() {
        grid.getStyle()
                .set("background", "white")
                .set("border-radius", "12px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "0 10px 26px rgba(15,23,42,.08)")
                .set("padding", "10px");
    }

    private void createGrid() {

        grid.addColumn(p -> p.getCliente().getNombre())
                .setHeader("Cliente")
                .setAutoWidth(true);

        grid.addColumn(p -> p.getCliente().getDireccion())
                .setHeader("Dirección de entrega")
                .setAutoWidth(true);

        grid.addComponentColumn(pedido -> {

            if (pedido.getEstado() == PedidoEstado.LISTO_REPARTO) {
                Button entregar = new Button("Entregar", VaadinIcon.TRUCK.create());
                entregar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

                entregar.addClickListener(e -> {
                    pedido.setEstado(PedidoEstado.EN_REPARTO);
                    gestionarPedido.save(pedido);
                    loadData();
                });

                return entregar;
            }

            if (pedido.getEstado() == PedidoEstado.EN_REPARTO) {
                Button entregar = new Button("Entregado", VaadinIcon.CHECK_CIRCLE.create());
                entregar.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

                entregar.addClickListener(e -> {
                    pedido.setEstado(PedidoEstado.ENTREGADO);
                    gestionarPedido.save(pedido);
                    loadData();
                });

                return entregar;
            }

            return new Span("—");

        }).setHeader("Acción");
    }

    /* ========================= DATA ========================= */

    private void loadData() {
        Set<Pedido> pedidos = gestionarPedido.pedidos_por_estado(estadoFiltro);
        grid.setItems(pedidos);
    }
}
