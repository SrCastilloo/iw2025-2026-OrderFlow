package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import es.uca.orderflow.business.entities.Detalle_Carrito;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.services.GestionarCarrito;
import es.uca.orderflow.persistence.data.ClienteRepository;

import java.util.List;

@Route("/Carro")
@AnonymousAllowed
public class CarritoView extends VerticalLayout {

    private final GestionarCarrito gestionarCarrito;
    private final ClienteRepository clienteRepository;
    private Long clienteId;

    private final Div cartContainer = new Div();
    private final Div footerBar = new Div();

    public CarritoView(GestionarCarrito gestionarCarrito, ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
        this.gestionarCarrito = gestionarCarrito;

        clienteId = (Long) VaadinSession.getCurrent().getAttribute("clienteId");
        if (clienteId == null) {
            Notification.show("Debes iniciar sesión primero");
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)");

        add(buildTopBar());
        add(buildCartList());
        add(buildFooter());     // <-- pie una sola vez
        renderCartItems();      // <-- solo rellena datos
    }

    /* ----------- BARRA SUPERIOR ----------- */
    private Component buildTopBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setPadding(true);
        bar.setAlignItems(Alignment.CENTER);
        bar.getStyle().set("background", "rgba(255,255,255,.88)")
                .set("backdrop-filter", "blur(8px)")
                .set("border-bottom", "1px solid #eef2f7")
                .set("position", "sticky")
                .set("top", "0")
                .set("z-index", "10");

        Span title = new Span("🛒 Mi Carrito");
        title.getStyle().set("font-size", "20px").set("font-weight", "800");

        Button volver = new Button("Seguir comprando", VaadinIcon.SHOP.create(),
                e -> UI.getCurrent().navigate("home"));
        volver.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        bar.add(title);
        bar.expand(title);
        bar.add(volver);

        return bar;
    }

    /* ----------- LISTA DE PRODUCTOS ----------- */
    private Component buildCartList() {
        cartContainer.getStyle()
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("gap", "14px")
                .set("padding", "20px")
                .set("width", "100%");

        Scroller scroller = new Scroller(cartContainer);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);
        return scroller;
    }

    /* ----------- PIE: TOTAL + FINALIZAR ----------- */
    private Component buildFooter() {
        footerBar.getStyle()
                .set("background", "white")
                .set("border-top", "1px solid #eef2f7")
                .set("padding", "14px 20px")
                .set("display", "flex")
                .set("gap", "10px")
                .set("align-items", "center");

        // se rellena en renderCartItems()
        return footerBar;
    }

    /* ----------- DATA + RENDER ----------- */
    private void renderCartItems() {
        cartContainer.removeAll();

        List<Detalle_Carrito> carrito = gestionarCarrito.GetCarrito(clienteId);
        carrito.forEach(item -> cartContainer.add(cartItemCard(item)));

        // ---- actualizar pie (total + botón)
        footerBar.removeAll();
        double total = carrito.stream()
                .mapToDouble(i -> i.getProducto().getPrecio().doubleValue() * i.getCantidad())
                .sum();

        Span totalLbl = new Span("Total: " + String.format("%.2f", total) + " €");
        totalLbl.getStyle().set("font-weight", "800");

        Button finalizarCompra = new Button("✅ Finalizar pedido", VaadinIcon.CHECK.create(),
                e -> Notification.show("Funcionalidad pronto ✨"));
        finalizarCompra.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        finalizarCompra.getStyle().set("margin-left", "auto");

        footerBar.add(totalLbl, finalizarCompra);
    }

    /* ----------- CARD DE ITEM ----------- */
    private Component cartItemCard(Detalle_Carrito item) {
        Producto p = item.getProducto();

        Div card = new Div();
        card.getStyle().set("background", "white")
                .set("border-radius", "12px")
                .set("padding", "16px")
                .set("display", "flex")
                .set("justify-content", "space-between")
                .set("align-items", "center")
                .set("box-shadow", "0 4px 10px rgba(0,0,0,.1)");

        Span info = new Span(p.getNombre() + " - " + p.getPrecio() + " € x" + item.getCantidad());
        info.getStyle().set("font-weight", "600");

        Button eliminar = new Button("Eliminar", VaadinIcon.TRASH.create());
        eliminar.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        eliminar.addClickListener(e -> {
            try {
                // Usa los mismos ids que tenías en la versión con Grid
                gestionarCarrito.eliminarProductoCarrito(p.getId(), item.getCarrito().getId());
                Notification.show("Producto eliminado del carrito");
                renderCartItems(); // refrescar vista
            } catch (Exception ex) {
                Notification n = Notification.show("No se pudo eliminar: " + ex.getMessage());
                n.addThemeVariants(com.vaadin.flow.component.notification.NotificationVariant.LUMO_ERROR);
            }
        });

        card.add(info, eliminar);
        return card;
    }
}

