package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
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

    public PanelRepartidorView(GestionarPedido gestionarPedido) {
        this.gestionarPedido = gestionarPedido;

        /* ======== ESTILO GENERAL ======== */
        setId("cook-root");
        setPadding(false);
        setSpacing(false);
        setWidthFull();

        getStyle().set("background",
                "radial-gradient(1200px 600px at 50% -200px, rgba(255,255,255,.75), rgba(255,255,255,0))," +
                        "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)");

        createGrid();

        add(buildTopBar(), buildWrapper());
        loadData();
    }

    /* ========================= TOP BAR ========================= */

    private Component buildTopBar() {
        Div band = new Div();
        band.setWidthFull();
        band.setId("cook-band");

        band.getStyle()
                .set("position", "sticky")
                .set("top", "0")
                .set("z-index", "50")
                .set("background", "linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.86))")
                .set("backdrop-filter", "blur(10px) saturate(1.05)")
                .set("border-bottom", "1px solid #eef2f7")
                .set("box-shadow", "0 3px 18px rgba(15,23,42,.06)")
                .set("padding", "12px");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);

        H2 title = new H2("Panel del Repartidor");
        title.getStyle()
                .set("margin", "0")
                .set("font-weight", "900");

        bar.add(title);
        band.add(bar);

        return band;
    }

    /* ========================= CONTENEDOR PRINCIPAL ========================= */

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
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("box-shadow", "0 10px 26px rgba(15,23,42,.08)")
                .set("padding", "10px");
    }

    /* ========================= GRID ========================= */

    private void createGrid() {

        grid.addColumn(p -> p.getCliente().getNombre())
                .setHeader("Cliente")
                .setAutoWidth(true);

        grid.addColumn(p -> p.getCliente().getDireccion())
                .setHeader("Dirección de entrega")
                .setAutoWidth(true);

        grid.addComponentColumn(pedido -> {
            Button entregar = new Button("Entregar", VaadinIcon.TRUCK.create());
            entregar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            entregar.getStyle()
                    .set("border-radius", "12px")
                    .set("font-weight", "700");

            entregar.addClickListener(e -> {
                pedido.setEstado(PedidoEstado.EN_REPARTO);
                gestionarPedido.save(pedido);
                grid.getDataProvider().refreshItem(pedido);

                Notification.show(
                        "Pedido marcado como EN REPARTO",
                        3000,
                        Notification.Position.TOP_CENTER
                );
            });

            return entregar;
        }).setHeader("Acción");
    }

    /* ========================= DATA ========================= */

    private void loadData() {
        Set<Pedido> pedidos =
                gestionarPedido.pedidos_por_estado(PedidoEstado.LISTO_REPARTO);

        grid.setItems(pedidos);
    }
}
