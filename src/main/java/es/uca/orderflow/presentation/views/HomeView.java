package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;

import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.services.GestionarProducto;
import es.uca.orderflow.business.services.InsertarProductoCarrito;

import java.util.List;


@Route("/home")
@PageTitle("Tienda")
@AnonymousAllowed
public class HomeView extends VerticalLayout {

    private GestionarProducto gestionarProducto;
    private InsertarProductoCarrito productosToCarrito;

    private Div catalogGrid = new Div();
    private Long clienteId;

    public HomeView(GestionarProducto gestionarProducto, InsertarProductoCarrito productosToCarrito) {
        this.gestionarProducto = gestionarProducto;
        this.productosToCarrito = productosToCarrito;

        clienteId = (Long) VaadinSession.getCurrent().getAttribute("clienteId");
        if (clienteId == null) {
            Notification.show("Debes iniciar sesión primero");
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }

        setId("cliente-root");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background",
                "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)");

        add(buildTopBar());
        add(buildCatalog());
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const css=`" +
                        "@media(max-width:1400px){ .grid-cards{grid-template-columns:repeat(3,1fr)} }" +
                        "@media(max-width:1000px){ .grid-cards{grid-template-columns:repeat(2,1fr)} }" +
                        "@media(max-width:600px){ .grid-cards{grid-template-columns:repeat(1,1fr)} }`;" +
                        "if(!document.getElementById('grid-responsive')){" +
                        "const s=document.createElement('style');" +
                        "s.id='grid-responsive';" +
                        "s.textContent=css;" +
                        "document.head.appendChild(s);" +
                        "}" )
        );
        catalogGrid.addClassName("grid-cards");
        renderProducts();
    }

    /* ========================= TOP BAR ========================= */

    private Component buildTopBar() {
        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(Alignment.CENTER);
        bar.setPadding(true);
        bar.getStyle()
                .set("background", "rgba(255,255,255,.88)")
                .set("backdrop-filter", "blur(8px)")
                .set("position", "sticky")
                .set("top", "0")
                .set("z-index", "10")
                .set("border-bottom", "1px solid #eef2f7");

        Span brand = new Span("🍕 OderFLow Tienda");
        brand.getStyle().set("font-weight", "800").set("font-size", "18px");

        Button pedidos = new Button("Pedidos", VaadinIcon.CART_O.create(),
                e -> UI.getCurrent().navigate(PedidosView.class));

        Button carrito = new Button("Carrito", VaadinIcon.CART.create(),
                e -> UI.getCurrent().navigate(CarritoView.class));

        Button perfil = new Button(VaadinIcon.USER.create());
        perfil.setTooltipText("Editar perfil");
        perfil.addClickListener(e -> UI.getCurrent().navigate("modificar-cliente"));

        Button salir = new Button(VaadinIcon.EXIT.create());
        salir.getStyle().set("margin-left", "auto");
        salir.addClickListener(e -> {
            VaadinSession.getCurrent().close();
            UI.getCurrent().getPage().setLocation("login");
        });

        bar.add(brand);
        bar.expand(brand);
        bar.add(pedidos, carrito, perfil, salir);
        return bar;
    }

    /* ========================= GRID de TARJETAS ========================= */

    private Component buildCatalog() {

        catalogGrid.getStyle()
                .set("display", "grid")
                .set("width", "100%")
                .set("grid-template-columns", "repeat(auto-fill, minmax(300px, 1fr))")
                .set("gap", "20px")
                .set("padding", "20px");

        Scroller scroller = new Scroller(catalogGrid);
        scroller.setSizeFull(); // ✅ Usar toda la pantalla
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        return scroller;
    }

    private void renderProducts() {
        catalogGrid.removeAll();

        List<Producto> productos = gestionarProducto.getAllProductos();

        productos.forEach(p ->
                catalogGrid.add(productCard(p))
        );
    }

    /* ========================= CARD de PRODUCTO ========================= */

    private Component productCard(Producto p) {
        Div card = new Div();
        card.getStyle()
                .set("background", "white")
                .set("border-radius", "14px")
                .set("border", "1px solid #e2e8f0")
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-shadow", "0 6px 18px rgba(15,23,42,.1)")
                .set("transition", "transform .14s ease, box-shadow .14s ease");

        card.getElement().addEventListener("mouseenter", e -> {
            card.getStyle()
                    .set("transform", "translateY(-3px)")
                    .set("box-shadow", "0 18px 40px rgba(15,23,42,.18)");
        });
        card.getElement().addEventListener("mouseleave", e -> {
            card.getStyle()
                    .set("transform", "none")
                    .set("box-shadow", "0 6px 18px rgba(15,23,42,.1)");
        });

        // Body
        Div body = new Div();
        body.getStyle().set("padding", "18px");

        Span title = new Span(p.getNombre());
        title.getStyle()
                .set("font-weight", "800")
                .set("font-size", "18px");

        Span desc = new Span(p.getDescripcion());
        desc.getStyle()
                .set("color", "gray")
                .set("font-size", "14px");

        Span price = new Span(p.getPrecio() + " €");
        price.getStyle()
                .set("color", "#059669")
                .set("font-weight", "800")
                .set("font-size", "17px");

        Button add = new Button("Añadir al carrito", VaadinIcon.PLUS.create());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.setWidthFull();
        add.addClickListener(e -> {
            try {
                productosToCarrito.meterProductoCarrito(clienteId, p.getId(), 1);
                Notification.show("✅ " + p.getNombre() + " añadido al carrito",
                        3000, Notification.Position.TOP_CENTER);
            } catch (Exception ex) {
                Notification.show("❌ Error: " + ex.getMessage(),
                        4000, Notification.Position.MIDDLE);
            }
        });

        body.add(title, desc, price, add);
        card.add(body);

        return card;
    }

}

