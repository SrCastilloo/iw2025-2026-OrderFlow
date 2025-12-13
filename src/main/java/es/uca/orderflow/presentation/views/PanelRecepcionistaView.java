package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Empleado;
import es.uca.orderflow.business.entities.EstadoMesa;
import es.uca.orderflow.business.entities.Mesa;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.services.GestionarEmpleado;
import es.uca.orderflow.business.services.GestionarMesa;
import es.uca.orderflow.business.services.GestionarPedido;
import es.uca.orderflow.business.services.GestionarProducto;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.stream.Collectors;

@PageTitle("Panel Recepcionista")
@Route("/backoffice/recepcionista")
@AnonymousAllowed // de momento
public class PanelRecepcionistaView extends VerticalLayout {

    private final GestionarPedido gestionarPedido;
    private final GestionarProducto gestionarProducto;
    private final GestionarEmpleado gestionarEmpleado;
    private final GestionarMesa gestionarMesa;          // <<< NUEVO
    private final Empleado empleado;

    private final int pageSize = 12;
    private int page = 1;

    private final Div grid = new Div();
    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));
    private final Span counter = new Span();

    private List<Producto> todosProductos = new ArrayList<>();
    private List<Producto> productosP = new ArrayList<>();

    public PanelRecepcionistaView(GestionarPedido gestionarPedido,
                                  GestionarProducto gestionarProducto,
                                  GestionarEmpleado gestionarEmpleado,
                                  GestionarMesa gestionarMesa) {        // <<< NUEVO PARÁMETRO

        this.gestionarPedido = gestionarPedido;
        this.gestionarProducto = gestionarProducto;
        this.gestionarEmpleado = gestionarEmpleado;
        this.gestionarMesa = gestionarMesa;                            // <<< NUEVO
        this.empleado = (Empleado) VaadinSession.getCurrent().getAttribute("empleadoLogueado");

        // Si no hay empleado en sesión, redirigimos a login
        if (empleado == null) {
            UI.getCurrent().navigate("/login");
            return;
        }

        // Cargar productos
        todosProductos = gestionarProducto.consultarProductos();
        productosP = new ArrayList<>(todosProductos);

        // ====== LAYOUT GENERAL ======
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(Alignment.STRETCH);
        getStyle().set("background", "var(--lumo-base-color)");

        Button caja = navChip("Caja", VaadinIcon.CASH, () -> navigate("/backoffice/caja"));
        // ====== NAVBAR SUPERIOR ======
        HorizontalLayout menu = new HorizontalLayout();
        menu.setWidthFull();
        menu.setSpacing(true);
        menu.setPadding(false);
        menu.setJustifyContentMode(JustifyContentMode.END);
        menu.getStyle()
                .set("background", "#020617")
                .set("padding", "0.4rem 1.5rem")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.35)")
                .set("position", "sticky")
                .set("top", "0")
                .set("z-index", "100");

        Button pedidos = navChip("Nuevo Pedido", VaadinIcon.PENCIL, () -> navigate("/backoffice/crearpedido"));
        Button mesas = navChip("Mesas", VaadinIcon.GRID, this::openMesasDialog);      // <<< NUEVO
        Button perfil = navChip("Mi perfil", VaadinIcon.USER, () -> navigate("/backoffice/empleado/perfil"));
        Button salir = navChip("Salir", VaadinIcon.EXIT, () -> {
            VaadinSession.getCurrent().close();
            navigate("/login");
        });
        menu.add(pedidos, mesas, caja,perfil, salir);                                      // <<< añadido "mesas"

        add(menu);

        // ====== WRAPPER CENTRAL ======
        Div wrapper = new Div();
        wrapper.getStyle()
                .set("width", "100%")
                .set("max-width", "1200px")
                .set("margin", "0 auto")
                .set("padding", "1.25rem 1.5rem 2.5rem");

        // ====== CABECERA: BIENVENIDA ======
        H2 titulo = new H2("Bienvenido, " + empleado.getNombre());
        titulo.getStyle()
                .set("font-size", "2.2rem")
                .set("font-weight", "800")
                .set("margin", "1rem 0 0.3rem 0")
                .set("color", "var(--lumo-header-text-color)")
                .set("letter-spacing", "-0.04em");

        Span subtitulo = new Span("Gestiona los pedidos y productos de forma rápida y visual.");
        subtitulo.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-m)");

        VerticalLayout headerBlock = new VerticalLayout(titulo, subtitulo);
        headerBlock.setPadding(false);
        headerBlock.setSpacing(false);
        headerBlock.setWidthFull();
        headerBlock.getStyle().set("margin-bottom", "1.5rem");

        wrapper.add(headerBlock);

        // ====== SECCIÓN PRODUCTOS ======
        H3 productos = new H3("Productos a la carta");
        productos.getStyle()
                .set("font-size", "1.6rem")
                .set("font-weight", "700")
                .set("margin", "0 0 0.5rem 0")
                .set("color", "#111827")
                .set("letter-spacing", "-0.03em");

        wrapper.add(productos);

        // ====== BUSCADOR + CONTADOR ======
        TextField buscador = new TextField();
        buscador.setPlaceholder("Buscar producto por nombre...");
        buscador.setClearButtonVisible(true);
        buscador.setWidthFull();
        buscador.setPrefixComponent(VaadinIcon.SEARCH.create());
        buscador.getStyle().set("max-width", "420px");

        counter.getStyle()
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        HorizontalLayout buscadorBar = new HorizontalLayout(buscador, counter);
        buscadorBar.setWidthFull();
        buscadorBar.setAlignItems(Alignment.END);
        buscadorBar.expand(buscador);
        buscadorBar.getStyle().set("margin-bottom", "0.75rem");

        wrapper.add(buscadorBar);

        // Lógica de filtrado
        buscador.addValueChangeListener(e -> {
            String term = e.getValue() == null ? "" : e.getValue().trim().toLowerCase();

            if (term.isEmpty()) {
                productosP = new ArrayList<>(todosProductos);
            } else {
                productosP = todosProductos.stream()
                        .filter(p -> p.getNombre() != null &&
                                p.getNombre().toLowerCase().contains(term))
                        .collect(Collectors.toList());
            }
            page = 1;
            renderPage();
        });

        // ====== GRID DE TARJETAS ======
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fill, minmax(260px, 1fr))")
                .set("gap", "1.5rem")
                .set("width", "100%")
                .set("margin-top", "0.75rem");

        wrapper.add(grid);

        // ====== PAGINADOR ======
        Component pag = buildPager();
        wrapper.add(pag);

        add(wrapper);

        // Pintar primera página
        renderPage();
    }

    /* ================= NAV CHIPS ================= */

    private Button navChip(String text, VaadinIcon icon, Runnable action) {
        Button b = new Button(text, icon.create());
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        b.getStyle()
                .set("color", "white")
                .set("background", "#111827")
                .set("border-radius", "999px")
                .set("padding", "6px 14px")
                .set("font-size", "var(--lumo-font-size-s)")
                .set("font-weight", "500")
                .set("border", "1px solid rgba(148,163,184,0.5)")
                .set("box-shadow", "0 0 0 1px rgba(15,23,42,0.75)")
                .set("transition", "background .14s ease, transform .14s ease, box-shadow .14s ease");

        b.getElement().addEventListener("mouseenter", e -> {
            b.getStyle()
                    .set("background", "#1f2937")
                    .set("transform", "translateY(-1px)")
                    .set("box-shadow", "0 8px 18px rgba(15,23,42,0.7)");
        });
        b.getElement().addEventListener("mouseleave", e -> {
            b.getStyle()
                    .set("background", "#111827")
                    .set("transform", "none")
                    .set("box-shadow", "0 0 0 1px rgba(15,23,42,0.75)");
        });
        b.addClickListener(e -> action.run());
        return b;
    }

    private void navigate(String route) {
        UI.getCurrent().navigate(route);
    }

    // --- Navegación con producto en sesión ---
    private void navigate(String route, Producto producto) {
        VaadinSession.getCurrent().setAttribute("productoAñadirTemporal", producto);
        UI.getCurrent().navigate(route);
    }

    private String formatPrice(BigDecimal p) {
        return p == null ? "—" : euro.format(p);
    }

    /* ========================= PAGINADOR ========================= */

    private Component buildPager() {
        HorizontalLayout pager = new HorizontalLayout();
        pager.setWidthFull();
        pager.setJustifyContentMode(JustifyContentMode.CENTER);
        pager.setPadding(true);
        pager.getStyle().set("margin-top", "1.25rem");

        Button prev = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> {
            page = Math.max(1, page - 1);
            renderPage();
        });
        Button next = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> {
            page = Math.min(maxPage(), page + 1);
            renderPage();
        });
        prev.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Span lbl = new Span();
        lbl.getStyle()
                .set("min-width", "160px")
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        pager.add(prev, lbl, next);
        pager.getElement().setProperty("role", "pager");
        return pager;
    }

    private int maxPage() {
        if (productosP.isEmpty()) return 1;
        return (int) Math.ceil((double) productosP.size() / pageSize);
    }

    private void renderPage() {
        grid.removeAll();

        if (productosP.isEmpty()) {
            counter.setText("0 productos encontrados");

            Div empty = new Div();
            empty.getStyle()
                    .set("padding", "24px")
                    .set("border", "1px dashed var(--lumo-contrast-10pct)")
                    .set("border-radius", "12px")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("background", "var(--lumo-base-color)")
                    .set("text-align", "center")
                    .set("box-shadow", "0 6px 16px rgba(15,23,42,.06)");
            empty.add(new H4("No hay resultados"),
                    new Paragraph("Prueba ajustando la búsqueda o limpia los filtros."));
            grid.add(empty);
        } else {
            int from = (page - 1) * pageSize;
            int to = Math.min(from + pageSize, productosP.size());
            productosP.subList(from, to).forEach(p -> grid.add(productCard(p)));

            counter.setText("Mostrando " + (from + 1) + "–" + to + " de " + productosP.size());

            getChildren()
                    .filter(c -> "pager".equals(c.getElement().getProperty("role")))
                    .findFirst()
                    .ifPresent(pager -> {
                        List<Component> kids = pager.getChildren().collect(Collectors.toList());
                        if (kids.size() == 3) {
                            Button prev = (Button) kids.get(0);
                            Span lbl = (Span) kids.get(1);
                            Button next = (Button) kids.get(2);
                            prev.setEnabled(page > 1);
                            next.setEnabled(page < maxPage());
                            lbl.setText("Página " + page + " / " + maxPage());
                        }
                    });
        }
    }

    /* ===================== TARJETA PRODUCTO ====================== */

    private Component productCard(Producto p) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "18px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-shadow", "0 10px 30px rgba(15,23,42,.14)")
                .set("transition", "transform .16s ease, box-shadow .16s ease, border-color .18s ease");

        card.getElement().addEventListener("mouseenter", e ->
                card.getStyle()
                        .set("transform", "translateY(-4px)")
                        .set("box-shadow", "0 22px 45px rgba(15,23,42,.18)")
                        .set("border-color", "#a7f3d0"));
        card.getElement().addEventListener("mouseleave", e ->
                card.getStyle()
                        .set("transform", "none")
                        .set("box-shadow", "0 10px 30px rgba(15,23,42,.14)")
                        .set("border-color", "var(--lumo-contrast-10pct)"));

        // Imagen
        Div imgWrap = new Div();
        imgWrap.getStyle()
                .set("position", "relative")
                .set("aspect-ratio", "16/10")
                .set("height", "auto")
                .set("overflow", "hidden")
                .set("background", "var(--lumo-contrast-5pct)");

        Image img = buildImage(p.getFoto(), p.getNombre());
        img.setWidth("100%");
        img.setHeight("100%");
        img.getStyle()
                .set("object-fit", "cover")
                .set("transform", "scale(1)")
                .set("transition", "transform .25s ease");
        imgWrap.getElement().addEventListener("mouseenter",
                e -> img.getStyle().set("transform", "scale(1.045)"));
        imgWrap.getElement().addEventListener("mouseleave",
                e -> img.getStyle().set("transform", "scale(1)"));

        Div shine = new Div();
        shine.getStyle()
                .set("position", "absolute").set("inset", "0")
                .set("background", "linear-gradient(115deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.38) 45%, rgba(255,255,255,0) 60%)")
                .set("transform", "translateX(-120%)")
                .set("transition", "transform .6s ease");
        imgWrap.getElement().addEventListener("mouseenter",
                e -> shine.getStyle().set("transform", "translateX(120%)"));
        imgWrap.getElement().addEventListener("mouseleave",
                e -> shine.getStyle().set("transform", "translateX(-120%)"));

        Span price = new Span(formatPrice(p.getPrecio()));
        price.getStyle()
                .set("position", "absolute").set("left", "10px").set("bottom", "10px")
                .set("padding", "5px 11px")
                .set("border-radius", "999px")
                .set("background", "rgba(15,23,42,0.96)")
                .set("color", "#6ee7b7")
                .set("font-weight", "800")
                .set("font-size", "0.9rem")
                .set("letter-spacing", ".03em")
                .set("text-transform", "uppercase")
                .set("box-shadow", "0 10px 22px rgba(15,23,42,.55)");

        imgWrap.add(img, shine, price);

        // Cuerpo
        Div body = new Div();
        body.getStyle().set("padding", "12px 14px 8px");

        Span title = new Span(Objects.toString(p.getNombre(), "Producto"));
        title.getStyle()
                .set("display", "-webkit-box").set("-webkit-line-clamp", "1").set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden").set("font-weight", "800")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "1.05rem");

        Span desc = new Span(Objects.toString(p.getDescripcion(), "Sin descripción"));
        desc.getStyle()
                .set("display", "-webkit-box").set("-webkit-line-clamp", "2").set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        body.add(title, new Paragraph(), desc);

        // Acciones
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.getStyle().set("padding", "0 14px 14px");

        Button addBtn = new Button("Añadir al pedido", VaadinIcon.PLUS_CIRCLE.create());
        addBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addBtn.getStyle()
                .set("border-radius", "999px")
                .set("font-size", "var(--lumo-font-size-s)");

        addBtn.addClickListener(e -> navigate("/backoffice/crearpedido", p));

        actions.add(addBtn);

        card.add(imgWrap, body, actions);
        return card;
    }

    private Image buildImage(String foto, String alt) {
        Image img = new Image();
        img.setAlt(alt == null ? "producto" : alt);
        img.setWidth("100%");
        img.setHeight("100%");
        img.getStyle().set("object-fit", "cover");
        img.getElement().setAttribute("loading", "lazy");

        if (foto == null || foto.isBlank()) {
            img.setSrc("/images/default-product.jpg");
            return img;
        }

        String f = foto.trim();
        if (f.startsWith("http://") || f.startsWith("https://") || f.startsWith("data:image/")) {
            img.setSrc(f);
            return img;
        }
        String ctx = "";
        if (VaadinService.getCurrentRequest() != null)
            ctx = VaadinService.getCurrentRequest().getContextPath();

        String filename = f.substring(f.lastIndexOf('/') + 1);
        StreamResource sr = streamIfExists("static" + (f.startsWith("/") ? f : "/" + f));
        if (sr == null) sr = streamIfExists("static/" + filename);
        if (sr == null) sr = streamIfExists("static/images/products/" + filename);
        if (sr == null) sr = streamIfExists(filename);
        if (sr != null) {
            img.setSrc(sr);
            return img;
        }

        img.setSrc(f.startsWith("/") ? ctx + f : ctx + "/" + f);
        return img;
    }

    private StreamResource streamIfExists(String classpathPath) {
        String p = classpathPath.startsWith("/") ? classpathPath : "/" + classpathPath;
        if (getClass().getResource(p) == null) return null;
        return new StreamResource(p.substring(p.lastIndexOf('/') + 1),
                () -> getClass().getResourceAsStream(p));
    }

    /* =============== NUEVO: DIÁLOGO DE GESTIÓN DE MESAS ================= */

    private void openMesasDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Gestión de mesas");

        dialog.setWidth("600px");
        dialog.setResizable(false);

        Grid<Mesa> gridMesas = new Grid<>(Mesa.class, false);
        gridMesas.addColumn(Mesa::getNombre)
                .setHeader("Mesa")
                .setAutoWidth(true)
                .setFlexGrow(1);

        gridMesas.addColumn(m -> m.getEstado().name())
                .setHeader("Estado")
                .setAutoWidth(true);

        gridMesas.addComponentColumn(m -> {
            // --- Botón MARCAR LIBRE ---
            Button liberar = new Button("Marcar libre", e -> {
                gestionarMesa.marcarMesaLibre(m.getId());
                Notification.show("Mesa " + m.getNombre() + " marcada como LIBRE",
                                2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refreshMesasGrid(gridMesas);
            });
            liberar.addThemeVariants(
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_SUCCESS
            );
            liberar.setEnabled(m.getEstado() == EstadoMesa.OCUPADA);
            liberar.getStyle()
                    .set("minWidth", "130px")
                    .set("padding", "6px 16px")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("border-radius", "999px");

            // --- Botón MARCAR OCUPADA (más grande y visible) ---
            Button ocupar = new Button("Marcar ocupada", e -> {
                gestionarMesa.marcarMesaOcupada(m.getId());
                Notification.show("Mesa " + m.getNombre() + " marcada como OCUPADA",
                                2500, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                refreshMesasGrid(gridMesas);
            });
            ocupar.addThemeVariants(
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_ERROR
            );
            ocupar.setEnabled(m.getEstado() == EstadoMesa.LIBRE);
            ocupar.getStyle()
                    .set("minWidth", "500px")
                    .set("padding", "20px 40px")
                    .set("font-size", "var(--lumo-font-size-s)")
                    .set("border-radius", "999px");

            HorizontalLayout hl = new HorizontalLayout(liberar, ocupar);
            hl.setSpacing(true);
            hl.setAlignItems(Alignment.CENTER);
            return hl;
        }).setHeader("Acciones");

        gridMesas.setWidthFull();
        gridMesas.setHeight("350px");

        refreshMesasGrid(gridMesas);

        Button cerrar = new Button("Cerrar", e -> dialog.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.add(gridMesas);
        dialog.getFooter().add(cerrar);

        dialog.open();
    }


    private void refreshMesasGrid(Grid<Mesa> gridMesas) {
        gridMesas.setItems(gestionarMesa.obtenerTodasLasMesas());
    }
}
