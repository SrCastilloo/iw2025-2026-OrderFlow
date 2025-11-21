package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
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
import es.uca.orderflow.persistence.data.Detalle_PedidoRepository;
import es.uca.orderflow.persistence.data.PedidoRepository;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;

import java.text.NumberFormat;
import java.util.Locale;
import es.uca.orderflow.business.services.PedidoEstado;
import java.util.Set;

@PageTitle("Panel Cocinero")
@Route("/backoffice/cocinero")
@AnonymousAllowed //de momento
public class PanelCocineroView extends VerticalLayout {

    private final PedidoRepository pedidoRepository;
    private final GestionarPedido gestionarPedido;
    private final Detalle_PedidoRepository detallePedidoRepository;

    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es","ES"));

    private final Grid<Pedido> grid = new Grid<>(Pedido.class, false);

    public PanelCocineroView(PedidoRepository pedidoRepository,
                             GestionarPedido gestionarPedido,
                             Detalle_PedidoRepository detallePedidoRepository) {

        this.pedidoRepository = pedidoRepository;
        this.gestionarPedido = gestionarPedido;
        this.detallePedidoRepository = detallePedidoRepository;

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

        H2 title = new H2("Panel del Cocinero");
        title.getStyle().set("margin", "0").set("font-weight", "900");

        bar.add(title);
        band.add(bar);

        return band;
    }

    /* ========================= CONTENEDOR PRIMARIO ========================= */

    private Component buildWrapper() {
        Div wrap = new Div();
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "20px auto")
                .set("padding", "0 16px")
                .set("width", "100%");

        // MUY IMPORTANTE:
        grid.setWidth("100%");
        grid.setHeight("600px"); // <--- AQUI EL ARREGLO
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
        grid.addColumn(pedido -> pedido.getCliente().getNombre())
                .setHeader("Cliente")
                .setAutoWidth(true);

        grid.addColumn(Pedido::getFechaRealizacion)
                .setHeader("Fecha")
                .setAutoWidth(true);

        // Ver productos
        grid.addComponentColumn(pedido -> {
            Button ver = new Button("Ver productos", VaadinIcon.LIST.create());
            ver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            ver.getStyle()
                    .set("border-radius", "12px")
                    .set("padding", "6px 12px")
                    .set("font-weight", "600");
            ver.addClickListener(e -> openProductosDialog(pedido));
            return ver;
        }).setHeader("Productos");

        // Cambiar estado
        grid.addComponentColumn(pedido -> {
            Button editarEstadoButton = new Button("Preparar → Listo", VaadinIcon.CHECK.create());
            editarEstadoButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            editarEstadoButton.getStyle()
                    .set("border-radius", "12px")
                    .set("font-weight", "700");

            editarEstadoButton.addClickListener(e -> {
                pedido.setEstado(PedidoEstado.LISTO_REPARTO);
                gestionarPedido.save(pedido);
                grid.getDataProvider().refreshItem(pedido);

                Notification.show(
                        "Pedido marcado como LISTO PARA REPARTO",
                        3000,
                        Notification.Position.TOP_CENTER
                );
            });

            return editarEstadoButton;

        }).setHeader("Estado");
    }

    /* ========================= DATA ========================= */

    private void loadData() {
        Set<Pedido> pedidos = pedidoRepository.findByEstado(PedidoEstado.PREPARACION);
        grid.setItems(pedidos);
    }

    /* ========================= DIALOG DE PRODUCTOS ========================= */

    private void openProductosDialog(Pedido p) {
        Dialog dlg = new Dialog();
        dlg.setWidth("500px");
        dlg.getElement().getThemeList().add("no-padding");

        dlg.setHeaderTitle("Pedido #" + p.getId());

        /* ======== LISTA DE PRODUCTOS ======== */
        UnorderedList ul = new UnorderedList();
        var detalles = detallePedidoRepository.findByPedido(p);

        double total = 0d;

        for (var d : detalles) {
            double imp = d.getImporte() == null ? 0d : d.getImporte().doubleValue();
            total += imp;

            ListItem li = new ListItem(
                    d.getCantidad() + " × " +
                            (d.getProducto() == null ? "-" : d.getProducto().getNombre())
                            + " — " + euro.format(imp)
            );

            li.getStyle()
                    .set("padding", "6px 0")
                    .set("font-weight", "500");

            ul.add(li);
        }

        Paragraph tot = new Paragraph("TOTAL: " + euro.format(total));
        tot.getStyle()
                .set("font-weight", "900")
                .set("margin", "16px 0 0 0")
                .set("color", "#10b981");

        VerticalLayout content = new VerticalLayout(ul, tot);
        content.setPadding(true);
        content.getStyle().set("padding", "18px");

        content.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "12px")
                .set("box-shadow", "0 10px 25px rgba(15,23,42,.08)");

        dlg.add(content);

        /* ======== FOOTER ======== */
        Button cerrar = new Button("Cerrar", VaadinIcon.CLOSE_SMALL.create(), e -> dlg.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dlg.getFooter().add(cerrar);
        dlg.open();
    }
}
