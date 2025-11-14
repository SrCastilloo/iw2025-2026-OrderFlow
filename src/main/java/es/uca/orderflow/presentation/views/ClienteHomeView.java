package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.business.services.*;
import org.springframework.beans.factory.annotation.Autowired;

import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Inicio - Cliente")
@Route("/cliente")
@AnonymousAllowed
public class ClienteHomeView extends VerticalLayout implements BeforeEnterObserver {

    /* ========================= SERVICIOS ========================= */
    private final EmpresaInfoService empresaInfoService;
    private final GestionarProducto gestionarProducto;
    private final InsertarProductoCarrito insertarProductoCarrito;
    private final GestionarCarritoCliente gestionarCarritoCliente;
    private final ClienteSesionService clienteSesionService;

    /* ========================= ESTADO ========================= */
    private Cliente clienteActivo;
    private final Span badgeCarrito = new Span();

    // UI base
    private final Div grid = new Div();
    private final TextField search = new TextField();
    private final ComboBox<String> sortBy = new ComboBox<>();
    private final Span counter = new Span();

    // Paginación
    private final int pageSize = 12;
    private int page = 1;
    private List<Producto> filtered = new ArrayList<>();

    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

    // fondos
    private static final String LIGHT_BG =
            "radial-gradient(1200px 600px at 50% -200px, rgba(255,255,255,.75), rgba(255,255,255,0))," +
                    "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)";
    private static final String DARK_BG =
            "radial-gradient(1200px 600px at 50% -200px, rgba(16,24,39,.6), rgba(16,24,39,0))," +
                    "linear-gradient(180deg,#0b1220 0%, #0e1629 40%, #0b1220 100%)";

    @Autowired
    public ClienteHomeView(EmpresaInfoService empresaInfoService,
                           GestionarProducto gestionarProducto,
                           InsertarProductoCarrito insertarProductoCarrito,
                           GestionarCarritoCliente gestionarCarritoCliente,
                           ClienteSesionService clienteSesionService
                           ) {
        this.empresaInfoService = empresaInfoService;
        this.gestionarProducto = gestionarProducto;
        this.insertarProductoCarrito = insertarProductoCarrito;
        this.gestionarCarritoCliente = gestionarCarritoCliente;
        this.clienteSesionService = clienteSesionService;

        // Cliente “logueado”
        //this.clienteActivo = clienteSesionService.getActual();



        setId("client-root");

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", LIGHT_BG);

        add(buildTopBar(), buildHero(), buildToolbar(), buildCatalog(), buildPager(), buildFab());
        injectResponsiveCss();
        injectDarkThemeCss();
        initThemeToggle();

        // pipeline inicial
        reload();
        refreshCartBadge();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // obtenemos el cliente desde tu servicio de sesión
        this.clienteActivo = clienteSesionService.getActual();

        if (clienteActivo == null) {
            // nadie logueado -> mandamos al login
            event.forwardTo(LoginView.class);   // o event.forwardTo("/login");
        } else {
            // si quieres, aquí puedes refrescar el badge del carrito
            refreshCartBadge();
        }
    }



    /* ========================= TOPBAR ========================= */

    private Component buildTopBar() {
        Div band = new Div();
        band.setId("client-band");
        band.setWidthFull();
        band.getStyle()
                .set("position", "sticky")
                .set("left", "0").set("right", "0").set("top", "0")
                .set("z-index", "60")
                .set("padding", "0")
                .set("margin", "0")
                .set("backdrop-filter", "blur(10px) saturate(1.05)")
                .set("background", "linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.86))")
                .set("border-bottom", "1px solid #eef2f7")
                .set("box-shadow", "0 3px 18px rgba(15,23,42,.06)");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getStyle().set("padding-left", "12px").set("padding-right", "12px");

        // Brand
        Empresa emp = empresaInfoService.obtenerEmpresaActiva();
        String nombre = emp != null ? emp.getNombreComercial() : "FoodFlow";

        HorizontalLayout brand = new HorizontalLayout();
        brand.setAlignItems(FlexComponent.Alignment.CENTER);

        Image logo = buildImage(emp != null ? emp.getLogo() : null, "logo");
        logo.setWidth("28px"); logo.setHeight("28px");
        logo.getStyle().set("border-radius", "8px").set("background", "var(--lumo-contrast-5pct)");
        Span brandTxt = new Span(nombre);
        brandTxt.getStyle().set("font-weight", "800").set("font-size", "18px").set("margin-left", "8px");
        brand.add(logo, brandTxt);

        // Menu acciones
        HorizontalLayout menu = new HorizontalLayout();
        menu.setSpacing(true); menu.setPadding(false);

        Button pedidos = navChip("Mis pedidos", VaadinIcon.LIST, () -> navigate("/cliente/pedidos"));
        Button perfil  = navChip("Mi perfil", VaadinIcon.USER, () -> navigate("/cliente/perfil"));
        Button salir   = navChip("Salir", VaadinIcon.EXIT, () -> navigate("/logincliente"));

        // Carrito + badge
        Button carrito = navChip("Mi carrito", VaadinIcon.CART, () -> navigate("/cliente/carrito"));
        badgeCarrito.getStyle()
                .set("display", "none")
                .set("min-width", "18px")
                .set("height", "18px")
                .set("border-radius", "999px")
                .set("background", "#ef4444")
                .set("color", "white")
                .set("font-size", "11px")
                .set("font-weight", "800")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "0 6px");
        Div cartWrap = new Div(carrito, badgeCarrito);
        cartWrap.getStyle().set("display", "inline-flex").set("gap", "6px").set("align-items", "center");

        Button themeBtn = new Button(VaadinIcon.MOON_O.create());
        themeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeBtn.setAriaLabel("Cambiar tema");
        themeBtn.addClickListener(e -> toggleTheme());

        bar.add(brand);
        bar.expand(brand);
        bar.add(pedidos, cartWrap, perfil, themeBtn, salir);

        band.add(bar);
        return band;
    }

    private Button navChip(String text, VaadinIcon icon, Runnable action) {
        Button b = new Button(text, icon.create());
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        b.getStyle()
                .set("color", "var(--lumo-body-text-color)")
                .set("border-radius", "999px")
                .set("padding", "6px 10px")
                .set("transition", "transform .12s ease, box-shadow .12s ease");
        b.getElement().addEventListener("mouseenter", e -> {
            b.getStyle().set("box-shadow", "inset 0 -2px 0 0 #10b981");
            b.getStyle().set("transform", "translateY(-1px)");
        });
        b.getElement().addEventListener("mouseleave", e -> {
            b.getStyle().remove("box-shadow");
            b.getStyle().set("transform", "none");
        });
        b.addClickListener(e -> action.run());
        return b;
    }

    /* ========================= HERO ========================= */

    private Component buildHero() {
        Div wrap = new Div();
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "10px auto 0")
                .set("padding", "8px 16px 0");

        Div box = new Div();
        box.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "16px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("padding", "16px 18px")
                .set("box-shadow", "0 10px 26px rgba(15,23,42,.06)");

        H2 title = new H2("Explora y disfruta");
        title.getStyle().set("margin", "0").set("font-weight", "900");
        Paragraph sub = new Paragraph("Descubre nuestra carta y añade tus favoritos al carrito.");
        sub.getStyle().set("margin", "6px 0 0").set("color", "var(--lumo-secondary-text-color)");
        box.add(title, sub);
        wrap.add(box);
        return wrap;
    }

    /* ========================= TOOLBAR ========================= */

    private Component buildToolbar() {
        Div wrap = new Div();
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "10px auto 0")
                .set("padding", "0 16px");

        HorizontalLayout tb = new HorizontalLayout();
        tb.setWidthFull();
        tb.setPadding(true);
        tb.setAlignItems(FlexComponent.Alignment.CENTER);

        search.setPlaceholder("Buscar por nombre o descripción…");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setWidth("520px");
        search.addValueChangeListener(e -> applyPipeline());
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "window.addEventListener('keydown',e=>{if(e.key==='/'&&document.activeElement!==$0.inputElement){e.preventDefault();$0.inputElement.focus();}})", search));

        sortBy.setItems("Recomendados", "Precio ↑", "Precio ↓", "Nombre (A–Z)");
        sortBy.setValue("Recomendados");
        sortBy.setWidth("170px");
        sortBy.addValueChangeListener(e -> { page = 1; applyPipeline(); });

        Span sep = new Span("•");
        sep.getStyle().set("color", "var(--lumo-secondary-text-color)");
        counter.getStyle().set("color", "var(--lumo-secondary-text-color)");

        tb.add(search, sortBy, sep, counter);
        tb.expand(search);

        wrap.add(tb);
        return wrap;
    }

    /* ========================= CATÁLOGO ========================= */

    private Component buildCatalog() {
        Div outer = new Div();
        outer.getStyle()
                .set("max-width", "1280px")
                .set("margin", "0 auto")
                .set("padding", "0 16px 26px");

        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(3, 1fr)")
                .set("gap", "20px")
                .set("padding", "6px 0");
        grid.getElement().getClassList().add("client-grid");

        Scroller scroller = new Scroller(grid);
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        outer.add(scroller);
        return outer;
    }

    /* ========================= PAGINADOR ========================= */

    private Component buildPager() {
        HorizontalLayout pager = new HorizontalLayout();
        pager.setWidthFull();
        pager.setJustifyContentMode(JustifyContentMode.CENTER);
        pager.setPadding(true);

        Button prev = new Button(VaadinIcon.ANGLE_LEFT.create(), e -> { page = Math.max(1, page - 1); renderPage(); });
        Button next = new Button(VaadinIcon.ANGLE_RIGHT.create(), e -> { page = Math.min(maxPage(), page + 1); renderPage(); });
        prev.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Span lbl = new Span();
        lbl.getStyle().set("min-width", "160px").set("text-align", "center").set("color", "var(--lumo-secondary-text-color)");

        pager.add(prev, lbl, next);
        pager.getElement().setProperty("role", "pager");
        return pager;
    }

    private int maxPage() {
        if (filtered.isEmpty()) return 1;
        return (int) Math.ceil((double) filtered.size() / pageSize);
    }

    /* ========================= FAB ========================= */

    private Component buildFab() {
        Button fab = new Button(VaadinIcon.CART_O.create(), e -> navigate("/cliente/carrito"));
        fab.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        fab.getStyle()
                .set("position", "fixed")
                .set("right", "24px")
                .set("bottom", "24px")
                .set("width", "54px")
                .set("height", "54px")
                .set("border-radius", "50%")
                .set("box-shadow", "0 16px 40px rgba(16,185,129,.35)");
        fab.setAriaLabel("Abrir carrito");
        return fab;
    }

    /* ========================= DATA / PIPELINE ========================= */

    private void reload() {
        filtered = gestionarProducto.consultarProductos();
        page = 1;
        applyPipeline();
    }

    private void applyPipeline() {
        String q = Optional.ofNullable(search.getValue()).orElse("").trim().toLowerCase();
        filtered = gestionarProducto.consultarProductos().stream()
                .filter(p -> q.isBlank() || safe(p.getNombre()).contains(q) || safe(p.getDescripcion()).contains(q))
                .sorted(getComparator())
                .collect(Collectors.toList());

        page = 1;
        renderPage();
    }

    private Comparator<Producto> getComparator() {
        String v = Optional.ofNullable(sortBy.getValue()).orElse("Recomendados");
        switch (v) {
            case "Precio ↑":
                return Comparator.comparing(p -> Optional.ofNullable(p.getPrecio()).orElse(BigDecimal.ZERO));
            case "Precio ↓":
                return Comparator.comparing((Producto p) -> Optional.ofNullable(p.getPrecio()).orElse(BigDecimal.ZERO)).reversed();
            case "Nombre (A–Z)":
                return Comparator.comparing(p -> safe(p.getNombre()));
            default: // Recomendados
                return Comparator.comparing((Producto p) -> safe(p.getNombre()))
                        .thenComparing(p -> Optional.ofNullable(p.getPrecio()).orElse(BigDecimal.ZERO));
        }
    }

    private String safe(String s) { return s == null ? "" : s.toLowerCase(); }

    private void renderPage() {
        grid.removeAll();

        if (filtered.isEmpty()) {
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
            int to = Math.min(from + pageSize, filtered.size());
            filtered.subList(from, to).forEach(p -> grid.add(productCard(p)));

            counter.setText("Mostrando " + (from + 1) + "–" + to + " de " + filtered.size());
            getChildren().filter(c -> "pager".equals(c.getElement().getProperty("role"))).findFirst().ifPresent(pager -> {
                List<Component> kids = pager.getChildren().collect(Collectors.toList());
                Button prev = (Button) kids.get(0);
                Span lbl = (Span) kids.get(1);
                Button next = (Button) kids.get(2);
                prev.setEnabled(page > 1);
                next.setEnabled(page < maxPage());
                lbl.setText("Página " + page + " / " + maxPage());
            });
        }
    }

    /* ========================= CARTA DE PRODUCTO ========================= */

    private Component productCard(Producto p) {
        Div card = new Div();
        card.getStyle()
                .set("background", "var(--lumo-base-color)")
                .set("border-radius", "14px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("box-shadow", "0 8px 22px rgba(15,23,42,.08)")
                .set("transition", "transform .14s ease, box-shadow .14s ease, border-color .18s ease");
        card.getElement().addEventListener("mouseenter", e ->
                card.getStyle()
                        .set("transform", "translateY(-2px)")
                        .set("box-shadow", "0 18px 40px rgba(15,23,42,.14)")
                        .set("border-color", "#d1fae5"));
        card.getElement().addEventListener("mouseleave", e ->
                card.getStyle()
                        .set("transform", "none")
                        .set("box-shadow", "0 8px 22px rgba(15,23,42,.08)")
                        .set("border-color", "var(--lumo-contrast-10pct)"));

        Div imgWrap = new Div();
        imgWrap.getStyle()
                .set("position", "relative")
                .set("aspect-ratio", "16/10")
                .set("height", "auto")
                .set("overflow", "hidden")
                .set("background", "var(--lumo-contrast-5pct)");

        Image img = buildImage(p.getFoto(), p.getNombre());
        img.setWidth("100%"); img.setHeight("100%");
        img.getStyle()
                .set("object-fit", "cover")
                .set("transform", "scale(1)")
                .set("transition", "transform .25s ease");
        imgWrap.getElement().addEventListener("mouseenter", e -> img.getStyle().set("transform", "scale(1.035)"));
        imgWrap.getElement().addEventListener("mouseleave", e -> img.getStyle().set("transform", "scale(1)"));

        Div shine = new Div();
        shine.getStyle()
                .set("position", "absolute").set("inset", "0")
                .set("background", "linear-gradient(115deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.35) 45%, rgba(255,255,255,0) 60%)")
                .set("transform", "translateX(-120%)")
                .set("transition", "transform .6s ease");
        imgWrap.getElement().addEventListener("mouseenter", e -> shine.getStyle().set("transform", "translateX(120%)"));
        imgWrap.getElement().addEventListener("mouseleave", e -> shine.getStyle().set("transform", "translateX(-120%)"));

        Span price = new Span(formatPrice(p.getPrecio()));
        price.getStyle()
                .set("position", "absolute").set("left", "10px").set("bottom", "10px")
                .set("padding", "5px 10px")
                .set("border-radius", "10px")
                .set("background", "var(--lumo-base-color)")
                .set("color", "#059669")
                .set("font-weight", "800")
                .set("box-shadow", "0 8px 18px rgba(5,150,105,.22)");

        imgWrap.add(img, shine, price);

        Div body = new Div();
        body.getStyle().set("padding", "12px 14px 8px");
        Span title = new Span(Objects.toString(p.getNombre(), "Producto"));
        title.getStyle()
                .set("display", "-webkit-box").set("-webkit-line-clamp", "1").set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden").set("font-weight", "800")
                .set("color", "var(--lumo-body-text-color)");
        Span desc = new Span(Objects.toString(p.getDescripcion(), "Sin descripción"));
        desc.getStyle()
                .set("display", "-webkit-box").set("-webkit-line-clamp", "2").set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden").set("color", "var(--lumo-secondary-text-color)");
        body.add(title, new Paragraph(), desc);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setPadding(false); actions.setSpacing(true);
        actions.getStyle().set("padding", "0 14px 14px");

        Button info = new Button("Más información", VaadinIcon.INFO_CIRCLE.create(), e -> showProductDetails(p));
        info.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        info.getStyle().set("flex", "1").set("min-height", "36px").set("border-radius", "10px");

        Button add = new Button("Añadir", VaadinIcon.CART.create(), e -> {
            addToCart(p);
            refreshCartBadge();
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        add.getStyle().set("flex", "1").set("min-height", "36px").set("border-radius", "10px");

        actions.add(info, add);
        actions.setFlexGrow(1, info, add);

        card.add(imgWrap, body, actions);
        return card;
    }

    /* ========================= CARRITO ========================= */

    private void addToCart(Producto producto) {
        Cliente actual = clienteSesionService.getActual();
        if (actual == null) {
            Notification n = Notification.show("Inicia sesión para añadir productos.", 2500, Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            // Aseguramos que el cliente tiene carrito antes de usar el servicio existente
            gestionarCarritoCliente.asegurarCarrito(actual.getId());
            insertarProductoCarrito.meterProductoCarrito(actual.getId(), producto.getId(), 1);

            Notification n = Notification.show(producto.getNombre() + " añadido al carrito",
                    1800, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            Notification n = Notification.show("No se pudo añadir: " + ex.getMessage(),
                    3000, Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void refreshCartBadge() {
        if (clienteActivo == null) {
            badgeCarrito.setText("");
            badgeCarrito.getStyle().set("display", "none");
            return;
        }
        int count = gestionarCarritoCliente.contarLineas(clienteActivo.getId());
        badgeCarrito.setText(count > 0 ? String.valueOf(count) : "");
        badgeCarrito.getStyle().set("display", count > 0 ? "inline-flex" : "none");
    }

    /* ========================= DETALLE ========================= */

    private void showProductDetails(Producto p) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(Objects.toString(p.getNombre(), "Producto"));

        Image img = buildImage(p.getFoto(), p.getNombre());
        img.setWidth("100%");
        img.getStyle().set("border-radius", "14px").set("object-fit", "cover");

        Paragraph desc = new Paragraph(Objects.toString(p.getDescripcion(), "—"));
        Span precio = new Span("Precio: " + formatPrice(p.getPrecio()));
        precio.getStyle().set("font-weight", "800").set("color", "#059669");

        VerticalLayout box = new VerticalLayout(img, desc, precio);
        box.setPadding(false); box.setSpacing(true);

        Button cerrar = new Button("Cerrar", e -> dlg.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button add = new Button("Añadir al carrito", VaadinIcon.CART.create(), e -> {
            addToCart(p);
            refreshCartBadge();
            dlg.close();
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        dlg.add(box);
        dlg.getFooter().add(cerrar, add);
        dlg.setWidth("520px");
        dlg.open();
    }

    /* ========================= UTILIDADES ========================= */

    private Image buildImage(String foto, String alt) {
        Image img = new Image();
        img.setAlt(alt == null ? "producto" : alt);
        img.setWidth("100%");
        img.setHeight("100%");
        img.getStyle().set("object-fit", "cover");
        img.getElement().setAttribute("loading", "lazy");

        if (foto == null || foto.isBlank()) { img.setSrc("/images/default-product.jpg"); return img; }

        String f = foto.trim();
        if (f.startsWith("http://") || f.startsWith("https://") || f.startsWith("data:image/")) {
            img.setSrc(f); return img;
        }
        String ctx = "";
        if (VaadinService.getCurrentRequest() != null) ctx = VaadinService.getCurrentRequest().getContextPath();

        String filename = f.substring(f.lastIndexOf('/') + 1);
        StreamResource sr = streamIfExists("static" + (f.startsWith("/") ? f : "/" + f));
        if (sr == null) sr = streamIfExists("static/" + filename);
        if (sr == null) sr = streamIfExists("static/images/products/" + filename);
        if (sr == null) sr = streamIfExists(filename);
        if (sr != null) { img.setSrc(sr); return img; }

        img.setSrc(f.startsWith("/") ? ctx + f : ctx + "/" + f);
        return img;
    }

    private StreamResource streamIfExists(String classpathPath) {
        String p = classpathPath.startsWith("/") ? classpathPath : "/" + classpathPath;
        if (getClass().getResource(p) == null) return null;
        return new StreamResource(p.substring(p.lastIndexOf('/') + 1), () -> getClass().getResourceAsStream(p));
    }

    private String formatPrice(BigDecimal p) { return p == null ? "—" : euro.format(p); }

    private void navigate(String route) { UI.getCurrent().navigate(route); }

    /* ========================= TEMA OSCURO ========================= */

    private void initThemeToggle() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const stored=localStorage.getItem('client-theme');" +
                        "const prefers=window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light';" +
                        "const theme=stored||prefers;" +
                        "document.documentElement.setAttribute('data-theme', theme);" +
                        "if(theme==='dark'){document.documentElement.setAttribute('theme','dark');}else{document.documentElement.removeAttribute('theme');}" +
                        "const root=document.getElementById('client-root');" +
                        "const band=document.getElementById('client-band');" +
                        "if(theme==='dark'){ root.style.background=$0; band.style.background='linear-gradient(180deg, rgba(17,24,39,.82), rgba(17,24,39,.7))'; band.style.borderBottom='1px solid #1f2937'; }" +
                        "else{ root.style.background=$1; band.style.background='linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.86))'; band.style.borderBottom='1px solid #eef2f7'; }",
                DARK_BG, LIGHT_BG));
    }

    private void toggleTheme() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const el=document.documentElement;" +
                        "const cur=el.getAttribute('data-theme')==='dark'?'light':'dark';" +
                        "el.setAttribute('data-theme',cur); localStorage.setItem('client-theme',cur);" +
                        "if(cur==='dark'){el.setAttribute('theme','dark');}else{el.removeAttribute('theme');}" +
                        "const root=document.getElementById('client-root');" +
                        "const band=document.getElementById('client-band');" +
                        "if(cur==='dark'){ root.style.background=$0; band.style.background='linear-gradient(180deg, rgba(17,24,39,.82), rgba(17,24,39,.7))'; band.style.borderBottom='1px solid #1f2937'; }" +
                        "else{ root.style.background=$1; band.style.background='linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.86))'; band.style.borderBottom='1px solid #eef2f7'; }",
                DARK_BG, LIGHT_BG));
    }

    private void injectResponsiveCss() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const css=`@media(max-width:1100px){.client-grid{grid-template-columns:repeat(2,1fr)} }" +
                        "@media(max-width:680px){.client-grid{grid-template-columns:repeat(1,1fr)} }`;" +
                        "if(!document.getElementById('client-grid-css')){const s=document.createElement('style');s.id='client-grid-css';s.textContent=css;document.head.appendChild(s);}"));
    }

    private void injectDarkThemeCss() {
        String css =
                "[data-theme='dark'] .client-grid > div{background:#111827 !important;border-color:#1f2937 !important;box-shadow:0 10px 26px rgba(0,0,0,.5) !important;}" +
                        "[data-theme='dark'] .client-grid > div:hover{border-color:#10b981 !important;}" +
                        "[data-theme='dark'] .v-button[theme~='tertiary']{color:#e5e7eb !important;}" +
                        "[data-theme='dark'] #client-band{background:linear-gradient(180deg, rgba(17,24,39,.82), rgba(17,24,39,.7)) !important; border-bottom:1px solid #1f2937 !important;}";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('client-dark-css')){const s=document.createElement('style');s.id='client-dark-css';s.textContent=$0;document.head.appendChild(s);}", css));
    }
}
