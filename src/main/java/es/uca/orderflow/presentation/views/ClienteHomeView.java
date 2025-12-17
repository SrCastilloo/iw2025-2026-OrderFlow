package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.*;
import es.uca.orderflow.business.services.*;
import es.uca.orderflow.presentation.components.LanguageSelector;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.server.VaadinSession;

import com.vaadin.flow.i18n.I18NProvider;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

@PageTitle("Inicio - Cliente")
@Route("/cliente")
@AnonymousAllowed
@CssImport("./styles/cliente-home-responsive.css")
public class ClienteHomeView extends VerticalLayout implements BeforeEnterObserver {

    /* ========================= SERVICIOS ========================= */
    private final EmpresaInfoService empresaInfoService;
    private final GestionarProducto gestionarProducto;
    private final InsertarProductoCarrito insertarProductoCarrito;
    private final GestionarCarritoCliente gestionarCarritoCliente;
    private final ClienteSesionService clienteSesionService;
    private final Button menuToggle = new Button(VaadinIcon.MENU.create());
    private final Div menuItems = new Div();
    private final I18NProvider i18nProvider;
    private final NumberFormat euro;
    private final CajaService cajaService;
    private  final OfertaService ofertaService;
    private final GestionarMenu gestionarMenu;
    Button sobreNosotros = navChip(getTranslation("nav.about_us"), VaadinIcon.INFO_CIRCLE,
            () -> navigate("/cliente/sobre-nosotros"));
    private List<Producto> allItems = new ArrayList<>();
    private final Map<Long, BigDecimal> precioFinalCache = new HashMap<>();
    /* ========================= ESTADO ========================= */
    private Cliente clienteActivo;
    private final Span badgeCarrito = new Span();

    // UI base
    private final Div grid = new Div();
    private final TextField search = new TextField();
    private final ComboBox<String> sortBy = new ComboBox<>();
    private final Span counter = new Span();



    // Paginación
    private final int pageSize = 3;
    private int page = 1;
    private List<Producto> filtered = new ArrayList<>();


    // Fondos (adaptados a la paleta del Login)
    private static final String LIGHT_BG =
            "radial-gradient(1200px 600px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                    "radial-gradient(1000px 500px at 110% 10%, rgba(255,120,90,.35), transparent 60%)," +
                    "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)";

    private static final String DARK_BG =
            "radial-gradient(1000px 600px at 10% -10%, rgba(15,23,42,.95), transparent 60%)," +
                    "radial-gradient(1000px 600px at 120% 0%, rgba(148,27,17,.75), transparent 60%)," +
                    "linear-gradient(180deg,#020617 0%, #0b1120 40%, #020617 100%)";

    @Autowired
    public ClienteHomeView(EmpresaInfoService empresaInfoService,
                           GestionarProducto gestionarProducto,
                           InsertarProductoCarrito insertarProductoCarrito,
                           GestionarCarritoCliente gestionarCarritoCliente,
                           ClienteSesionService clienteSesionService,
                           I18NProvider i18nProvider,CajaService cajaService, GestionarMenu gestionarMenu, OfertaService ofertaService) {

        this.empresaInfoService = empresaInfoService;
        this.gestionarProducto = gestionarProducto;
        this.insertarProductoCarrito = insertarProductoCarrito;
        this.gestionarCarritoCliente = gestionarCarritoCliente;
        this.clienteSesionService = clienteSesionService;
        this.i18nProvider = i18nProvider;
        this.cajaService = cajaService;
        this.euro = NumberFormat.getCurrencyInstance(VaadinSession.getCurrent().getLocale());
        this.gestionarMenu = gestionarMenu;
        this.ofertaService = ofertaService;


        setId("client-root");
        addClassName("cliente-home-view");
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Misma paleta que LoginView
        getElement().getStyle().set("--lumo-primary-color", "hsl(14, 90%, 55%)");
        getElement().getStyle().set("--lumo-primary-text-color", "hsl(14, 90%, 32%)");
        getElement().getStyle().set("--lumo-success-color", "hsl(135, 60%, 38%)");
        getElement().getStyle().set("--lumo-error-color", "hsl(0, 85%, 55%)");
        getElement().getStyle().set("--lumo-border-radius-l", "1.2rem");
        getElement().getStyle().set("--lumo-border-radius-m", "1rem");

        // Estructura principal
        add(
                buildTopBar(),
                buildHero(),
                buildToolbar(),
                buildCatalog(),
                buildPager(),
                buildFab()
        );

        initThemeToggle();

        reload();
        refreshCartBadge();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        this.clienteActivo = clienteSesionService.getActual();

        if (clienteActivo == null) {
            event.forwardTo(LoginView.class);
        } else {
            refreshCartBadge();
        }
    }

    /* ========================= FAB ========================= */

    private Component buildFab() {
        Button fab = new Button(VaadinIcon.CART_O.create(), e -> navigate("/cliente/carrito"));
        fab.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        fab.getStyle()
                .set("position", "fixed")
                .set("right", "20px")
                .set("bottom", "20px")
                .set("width", "60px")
                .set("height", "60px")
                .set("border-radius", "999px")
                .set("box-shadow", "0 18px 45px rgba(16,185,129,.55)");
        fab.setAriaLabel(getTranslation("aria.open_cart"));
        return fab;
    }

    /* ========================= TOPBAR ========================= */

    private Component buildTopBar() {
        Div band = new Div();
        band.setId("client-band");
        band.addClassName("client-topbar");
        band.setWidthFull();
        band.getStyle()
                .set("position", "sticky")
                .set("left", "0")
                .set("right", "0")
                .set("top", "0")
                .set("z-index", "60")
                .set("padding", "0")
                .set("margin", "0")
                .set("backdrop-filter", "blur(14px) saturate(1.25)")
                .set("background", "rgba(255,255,255,0.9)")
                .set("border-bottom", "1px solid rgba(255,120,90,.18)")
                .set("box-shadow", "0 10px 34px rgba(255,120,90,.18)");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setPadding(false);
        bar.setSpacing(true);
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getStyle()
                .set("padding", "8px 16px")
                .set("max-width", "1280px")
                .set("margin", "0 auto");

        // Brand
        Empresa emp = empresaInfoService.obtenerEmpresaActiva();
        String nombre = emp != null ? emp.getNombreComercial() : getTranslation("empresa.default_name");

        HorizontalLayout brand = new HorizontalLayout();
        brand.addClassName("client-brand");
        brand.setAlignItems(FlexComponent.Alignment.CENTER);

        Image logo = buildImage(emp != null ? emp.getLogo() : null, "logo");
        logo.setWidth("32px");
        logo.setHeight("32px");
        logo.getStyle()
                .set("border-radius", "10px")
                .set("background", "rgba(255,255,255,0.9)")
                .set("box-shadow", "0 10px 26px rgba(0,0,0,0.18)");

        Span brandTxt = new Span(nombre);
        brandTxt.getStyle()
                .set("font-weight", "900")
                .set("font-size", "1.2rem")
                .set("margin-left", "8px");

        brand.add(logo, brandTxt);

        // Menú acciones
        Button pedidos = navChip(getTranslation("nav.my_orders"), VaadinIcon.LIST, () -> navigate("/cliente/pedidos"));
        Button perfil  = navChip(getTranslation("nav.my_profile"), VaadinIcon.USER, () -> navigate("/cliente/perfil"));
        Button salir   = navChip(getTranslation("nav.logout"), VaadinIcon.EXIT, () -> navigate("/login"));

        // Carrito + badge
        // Carrito + badge
        Button carrito = navChip(getTranslation("nav.my_cart"), VaadinIcon.CART, () -> navigate("/cliente/carrito"));

        badgeCarrito.getStyle()
                .set("display", "none")
                .set("position", "absolute")      // superpuesto al botón
                .set("right", "-4px")             // un pelín fuera a la derecha
                .set("top", "-6px")               // un poco arriba del botón
                .set("min-width", "18px")
                .set("height", "18px")
                .set("border-radius", "999px")
                .set("background", "var(--lumo-error-color)")
                .set("color", "white")
                .set("font-size", "11px")
                .set("font-weight", "800")
                .set("align-items", "center")
                .set("justify-content", "center")
                .set("padding", "0 6px");

        Div cartWrap = new Div(carrito, badgeCarrito);
        cartWrap.getStyle()
                .set("display", "inline-flex")
                .set("gap", "6px")
                .set("align-items", "flex-start")
                .set("position", "relative");

        // Botón de tema y selector de idioma
        Button themeBtn = new Button(VaadinIcon.MOON_O.create());
        themeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeBtn.setAriaLabel(getTranslation("aria.change_theme"));
        themeBtn.addClickListener(e -> toggleTheme());
        themeBtn.addClassName("client-theme-toggle");

        LanguageSelector langSelector = new LanguageSelector(this.i18nProvider);
        langSelector.addClassName("client-lang-selector");

        menuItems.add(pedidos, cartWrap, perfil, themeBtn, langSelector,sobreNosotros, salir);
        menuItems.addClassName("client-menu-items");

        // Botón Hamburguesa
        menuToggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        menuToggle.addClassName("client-menu-toggle");
        menuToggle.setAriaLabel(getTranslation("aria.open_menu"));
        menuToggle.addClickListener(e -> {
            boolean isOpen = band.getElement().getClassList().contains("menu-open");
            if (isOpen) {
                band.getElement().getClassList().remove("menu-open");
            } else {
                band.getElement().getClassList().add("menu-open");
            }
        });

        bar.add(brand);
        bar.expand(brand);
        bar.add(menuItems, menuToggle);

        band.add(bar);
        return band;
    }

    private Button navChip(String text, VaadinIcon icon, Runnable action) {
        Button b = new Button(text, icon.create());
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        b.addClassName("client-nav-chip");
        b.getStyle()
                .set("color", "var(--lumo-body-text-color)")
                .set("border-radius", "999px")
                .set("padding", "6px 12px")
                .set("transition", "all .18s ease");

        b.getElement().addEventListener("mouseenter", e -> {
            b.getStyle()
                    .set("background", "rgba(255,255,255,0.9)")
                    .set("box-shadow", "0 10px 26px rgba(0,0,0,0.18)")
                    .set("transform", "translateY(-1px)");
        });
        b.getElement().addEventListener("mouseleave", e -> {
            b.getStyle()
                    .remove("background")
                    .set("box-shadow", "none")
                    .set("transform", "none");
        });
        b.addClickListener(e -> action.run());
        return b;
    }

    /* ========================= HERO ========================= */

    private Component buildHero() {
        Div wrap = new Div();
        wrap.addClassName("client-hero-wrap");
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "16px auto 0")
                .set("padding", "0 16px");

        Div box = new Div();
        box.addClassName("client-hero-box");
        box.getStyle()
                .set("position", "relative")
                .set("overflow", "hidden");

        // Fondo abstracto
        Div pattern = new Div();
        pattern.addClassName("client-hero-pattern");
        pattern.getStyle()
                .set("position", "absolute")
                .set("inset", "0")
                .set("opacity", "0.25")
                .set("background-image",
                        "radial-gradient(circle at 0% 0%, rgba(255,255,255,.28), transparent 60%)," +
                                "radial-gradient(circle at 100% 0%, rgba(255,255,255,.18), transparent 60%)," +
                                "linear-gradient(135deg, rgba(255,255,255,.12) 25%, transparent 25%, transparent 50%, rgba(255,255,255,.12) 50%, rgba(255,255,255,.12) 75%, transparent 75%, transparent)");
        box.add(pattern);

        H2 title = new H2(" " + getTranslation("hero.title"));
        title.addClassName("client-hero-title");
        title.getStyle()
                .set("margin", "0")
                .set("font-weight", "900")
                .set("font-size", "clamp(26px, 3vw, 34px)")
                .set("color", "white")
                .set("position", "relative");

        Paragraph sub = new Paragraph(getTranslation("hero.subtitle"));
        sub.addClassName("client-hero-subtitle");
        sub.getStyle()
                .set("margin", "8px 0 0")
                .set("font-size", "clamp(14px, 2vw, 16px)")
                .set("color", "rgba(255,255,255,0.85)")
                .set("position", "relative");

        box.add(title, sub);
        wrap.add(box);
        return wrap;
    }

    /* ========================= TOOLBAR ========================= */

    private Component buildToolbar() {
        Div wrap = new Div();
        wrap.addClassName("client-toolbar-wrap");
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "18px auto 0")
                .set("padding", "0 16px");

        HorizontalLayout tb = new HorizontalLayout();
        tb.addClassName("client-toolbar-layout");
        tb.setWidthFull();
        tb.setPadding(false);
        tb.setAlignItems(FlexComponent.Alignment.CENTER);
        tb.setSpacing(false);

        Div barWrap = new Div();
        barWrap.addClassName("client-toolbar-bar");
        barWrap.getStyle()
                .set("display", "flex")
                .set("gap", "18px")
                .set("align-items", "center")
                .set("padding", "12px 18px")
                .set("border-radius", "999px")
                .set("background", "rgba(255,255,255,.9)")
                .set("border", "1px solid rgba(255,120,90,.25)")
                .set("box-shadow", "0 20px 55px rgba(15,23,42,.18)");

        search.setPlaceholder(getTranslation("toolbar.search_placeholder"));
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setWidthFull();
        search.addClassName("client-search-field");
        search.addValueChangeListener(e -> applyPipeline());

        sortBy.setItems(
                getTranslation("toolbar.sort_recommended"),
                getTranslation("toolbar.sort_price_asc"),
                getTranslation("toolbar.sort_price_desc"),
                getTranslation("toolbar.sort_name_asc")
        );
        sortBy.setValue(getTranslation("toolbar.sort_recommended"));
        sortBy.addClassName("client-sort-select");
        sortBy.addValueChangeListener(e -> { page = 1; applyPipeline(); });

        counter.addClassName("client-result-counter");
        counter.getStyle()
                .set("color", "var(--lumo-primary-text-color)")
                .set("font-weight", "700")
                .set("font-size", "0.95rem");

        barWrap.add(search, sortBy, counter);
        tb.add(barWrap);
        tb.setJustifyContentMode(JustifyContentMode.END);

        wrap.add(tb);
        return wrap;
    }

    /* ========================= CATÁLOGO ========================= */

    private Component buildCatalog() {
        Div outer = new Div();
        outer.addClassName("client-catalog-wrap");
        outer.getStyle()
                .set("max-width", "1280px")
                .set("margin", "8px auto 0")
                .set("padding", "0 16px 26px");

        grid.addClassName("client-grid");
        grid.getStyle()
                .set("display", "grid")
                .set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))")
                .set("gap", "24px")
                .set("padding", "20px 0");

        Scroller scroller = new Scroller(grid);
        scroller.addClassName("client-scroller");
        scroller.setSizeFull();
        scroller.setScrollDirection(Scroller.ScrollDirection.VERTICAL);

        outer.add(scroller);
        return outer;
    }



    /* ========================= CARTA DE PRODUCTO ========================= */

    private Component productCard(Producto p) {
        boolean isMenu = (p.getTipo() == ProductoTipo.MENU);

        Div card = new Div();
        card.addClassName("client-product-card");
        card.getStyle()
                .set("width", "100%")
                .set("overflow", "hidden")
                .set("display", "flex")
                .set("flex-direction", "column")
                .set("transition", "transform .25s ease, box-shadow .25s ease");

        card.getElement().addEventListener("mouseenter", e ->
                card.getStyle()
                        .set("transform", "translateY(-3px)")
                        .set("box-shadow", "0 26px 60px rgba(15,23,42,.30)"));
        card.getElement().addEventListener("mouseleave", e ->
                card.getStyle()
                        .set("transform", "none")
                        .set("box-shadow", "0 18px 45px rgba(15,23,42,.16)"));

        Div imgWrap = new Div();
        imgWrap.addClassName("client-card-img");
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
                .set("transition", "transform .35s ease");
        imgWrap.getElement().addEventListener("mouseenter",
                e -> img.getStyle().set("transform", "scale(1.05)"));
        imgWrap.getElement().addEventListener("mouseleave",
                e -> img.getStyle().set("transform", "scale(1)"));

        Div shine = new Div();
        shine.getStyle()
                .set("position", "absolute")
                .set("inset", "0")
                .set("background",
                        "linear-gradient(120deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.35) 45%, rgba(255,255,255,0) 60%)")
                .set("transform", "translateX(-130%)")
                .set("transition", "transform .6s ease");
        imgWrap.getElement().addEventListener("mouseenter",
                e -> shine.getStyle().set("transform", "translateX(130%)"));
        imgWrap.getElement().addEventListener("mouseleave",
                e -> shine.getStyle().set("transform", "translateX(-130%)"));

        // Precio con oferta
        var pi = ofertaService.precioParaProducto(p);
        String priceText = formatPrice(pi.finalPrice());

        // Badge MENÚ (arriba derecha)
        if (isMenu) {
            Span menuBadge = new Span("MENÚ");
            menuBadge.getStyle()
                    .set("position", "absolute")
                    .set("right", "14px")
                    .set("top", "14px")
                    .set("padding", "6px 10px")
                    .set("border-radius", "999px")
                    .set("background", "#111827")
                    .set("color", "white")
                    .set("font-weight", "900")
                    .set("font-size", "12px")
                    .set("box-shadow", "0 10px 26px rgba(0,0,0,.22)");
            imgWrap.add(menuBadge);
        }

        // Badge OFERTA (arriba izquierda)
        if (pi.hayOferta()) {
            Span badge = new Span("-" + pi.descuentoPct().stripTrailingZeros().toPlainString() + "%");
            badge.getStyle()
                    .set("position", "absolute")
                    .set("left", "14px")
                    .set("top", "14px")
                    .set("padding", "6px 10px")
                    .set("border-radius", "999px")
                    .set("background", "hsl(0,85%,55%)")
                    .set("color", "white")
                    .set("font-weight", "900")
                    .set("box-shadow", "0 10px 26px rgba(239,68,68,.35)");
            imgWrap.add(badge);
        }

        // Precio (evitar solapes)
        Span price = new Span(priceText);
        price.addClassName("client-card-price");
        price.getStyle()
                .set("position", "absolute")
                .set("padding", "6px 12px")
                .set("border-radius", "999px")
                .set("background", "hsl(221, 83%, 55%)")
                .set("color", "white")
                .set("font-weight", "900")
                .set("font-size", "0.95rem")
                .set("box-shadow", "0 10px 26px rgba(37,99,235,.45)");

        // Si es menú: precio debajo del badge MENÚ (derecha). Si no: arriba derecha.
        if (isMenu) {
            price.getStyle()
                    .set("right", "14px")
                    .set("top", "46px"); // debajo del MENÚ
        } else {
            price.getStyle()
                    .set("right", "14px")
                    .set("top", "14px");
        }

        imgWrap.add(img, shine, price);

        // Body
        Div body = new Div();
        body.addClassName("client-card-body");
        body.getStyle().set("padding", "16px 18px 12px");

        Span title = new Span(Objects.toString(p.getNombre(), getTranslation("product.default_name")));
        title.addClassName("client-card-title");
        title.getStyle()
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "1")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("font-weight", "900")
                .set("font-size", "1.15rem")
                .set("color", "var(--lumo-body-text-color)");

        body.add(title);

        // Precio base tachado (debajo del título)
        if (pi.hayOferta()) {
            Span old = new Span(formatPrice(pi.base()));
            old.getStyle()
                    .set("display", "block")
                    .set("margin-top", "6px")
                    .set("text-decoration", "line-through")
                    .set("opacity", "0.7")
                    .set("color", "var(--lumo-secondary-text-color)");
            body.add(old);
        }

        Span desc = new Span(Objects.toString(p.getDescripcion(), getTranslation("product.no_description")));
        desc.addClassName("client-card-desc");
        desc.getStyle()
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "2")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("margin-top", "4px");

        body.add(desc);

        // Actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.addClassName("client-card-actions");
        actions.setWidthFull();
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.getStyle().set("padding", "0 18px 18px");

        Button info = new Button(getTranslation("card.more_info"), VaadinIcon.INFO_CIRCLE.create(),
                e -> showProductDetails(p));
        info.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        info.getStyle()
                .set("flex", "1")
                .set("min-height", "44px")
                .set("border-radius", "16px");

        Button add = new Button(getTranslation("card.add_to_cart"), VaadinIcon.CART.create(),
                e -> {
                    if (!cajaService.isCajaAbierta()) {
                        Notification.show("Caja cerrada: no se pueden realizar pedidos ahora.", 2500,
                                        Notification.Position.TOP_CENTER)
                                .addThemeVariants(NotificationVariant.LUMO_ERROR);
                        return;
                    }
                    addToCart(p);
                    refreshCartBadge();
                });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        add.getStyle()
                .set("flex", "2")
                .set("min-height", "44px")
                .set("border-radius", "18px")
                .set("font-weight", "800");

        actions.add(info, add);
        actions.setFlexGrow(1, info);
        actions.setFlexGrow(2, add);

        card.add(imgWrap, body, actions);
        return card;
    }


    /* ========================= PAGER ========================= */

    private Component buildPager() {
        HorizontalLayout pager = new HorizontalLayout();
        pager.addClassName("client-pager");
        pager.setWidthFull();
        pager.setJustifyContentMode(JustifyContentMode.CENTER);
        pager.setPadding(true);

        Button prev = new Button(VaadinIcon.ANGLE_LEFT.create(),
                e -> { page = Math.max(1, page - 1); renderPage(); });
        Button next = new Button(VaadinIcon.ANGLE_RIGHT.create(),
                e -> { page = Math.min(maxPage(), page + 1); renderPage(); });
        prev.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        next.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Span lbl = new Span();
        lbl.addClassName("client-pager-label");
        lbl.getStyle()
                .set("min-width", "160px")
                .set("text-align", "center")
                .set("color", "var(--lumo-secondary-text-color)");

        pager.add(prev, lbl, next);
        pager.getElement().setProperty("role", "pager");
        return pager;
    }

    /* ========================= DETALLE ========================= */

    private void showProductDetails(Producto p) {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle(Objects.toString(p.getNombre(), getTranslation("product.default_name")));
        dlg.setWidth("clamp(300px, 80vw, 560px)");

        VerticalLayout box = new VerticalLayout();
        box.setPadding(false);
        box.setSpacing(true);
        box.addClassName("client-dialog-body");

        Image img = buildImage(p.getFoto(), p.getNombre());
        img.setWidth("100%");
        img.getStyle()
                .set("border-radius", "14px")
                .set("object-fit", "cover")
                .set("aspect-ratio", "16/10");

        // Precio con oferta (si aplica)
        var pi = ofertaService.precioParaProducto(p);
        BigDecimal finalPrice = pi.finalPrice();

        HorizontalLayout priceRow = new HorizontalLayout();
        priceRow.setWidthFull();
        priceRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        priceRow.setSpacing(true);
        priceRow.getStyle().set("margin-top", "6px");

        Span precio = new Span(getTranslation("dialog.price") + ": " + formatPrice(finalPrice));
        precio.getStyle()
                .set("font-weight", "900")
                .set("font-size", "1.2rem")
                .set("color", "var(--lumo-primary-color)");

        priceRow.add(precio);

        if (pi.hayOferta()) {
            Span old = new Span(formatPrice(pi.base()));
            old.getStyle()
                    .set("text-decoration", "line-through")
                    .set("opacity", "0.7")
                    .set("margin-left", "8px");

            Span badge = new Span("-" + pi.descuentoPct().stripTrailingZeros().toPlainString() + "%");
            badge.getStyle()
                    .set("padding", "4px 10px")
                    .set("border-radius", "999px")
                    .set("background", "hsl(0,85%,55%)")
                    .set("color", "white")
                    .set("font-weight", "900");

            priceRow.add(old, badge);
        }

        H4 descTitle = new H4(getTranslation("dialog.description"));
        descTitle.getStyle()
                .set("margin-top", "12px")
                .set("margin-bottom", "0");

        Paragraph desc = new Paragraph(Objects.toString(p.getDescripcion(), "—"));
        desc.getStyle().set("color", "var(--lumo-secondary-text-color)");

        box.add(img, priceRow, descTitle, desc);

        // Si es MENÚ, mostramos la composición
        if (p.getTipo() == ProductoTipo.MENU) {
            H4 compTitle = new H4(getTranslation("menu.contains")); // si no lo tienes, pon "Incluye"
            compTitle.getStyle()
                    .set("margin-top", "10px")
                    .set("margin-bottom", "0");

            VerticalLayout comp = new VerticalLayout();
            comp.setPadding(false);
            comp.setSpacing(false);
            comp.getStyle()
                    .set("border", "1px solid rgba(0,0,0,.08)")
                    .set("border-radius", "12px")
                    .set("padding", "10px 12px");

            List<MenuComposicion> items = gestionarMenu.composicion(p.getId());
            if (items == null || items.isEmpty()) {
                Span empty = new Span(getTranslation("menu.no_items")); // o "Este menú no tiene productos"
                empty.getStyle().set("color", "var(--lumo-secondary-text-color)");
                comp.add(empty);
            } else {
                for (MenuComposicion mc : items) {
                    Producto prod = mc.getProducto();

                    HorizontalLayout row = new HorizontalLayout();
                    row.setWidthFull();
                    row.setPadding(false);
                    row.setSpacing(true);
                    row.setAlignItems(FlexComponent.Alignment.CENTER);
                    row.getStyle()
                            .set("padding", "6px 0")
                            .set("border-bottom", "1px dashed rgba(0,0,0,.08)");

                    Span name = new Span(Objects.toString(prod != null ? prod.getNombre() : null, "Producto"));
                    name.getStyle()
                            .set("font-weight", "700")
                            .set("flex", "1");

                    int qty = mc.getCantidad() == null ? 1 : mc.getCantidad();
                    Span qtySpan = new Span("x" + qty);
                    qtySpan.getStyle()
                            .set("font-weight", "800")
                            .set("opacity", "0.85");

                    row.add(name, qtySpan);
                    comp.add(row);
                }

                // quita el borde inferior de la última fila (opcional)
                Component last = comp.getComponentAt(comp.getComponentCount() - 1);
                last.getElement().getStyle().remove("border-bottom");
            }

            box.add(compTitle, comp);
        }

        Button cerrar = new Button(getTranslation("dialog.close"), e -> dlg.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button add = new Button(getTranslation("dialog.add_to_cart_btn"), VaadinIcon.CART.create(), e -> {
            addToCart(p);
            refreshCartBadge();
            dlg.close();
        });
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        add.setEnabled(cajaService.isCajaAbierta());

        dlg.add(box);
        dlg.getFooter().add(cerrar, add);
        dlg.open();
    }


    /* ========================= UTILIDADES ========================= */

    /* ========================= UTILIDADES ========================= */

    private Image buildImage(String foto, String alt) {
        Image img = new Image();
        img.setAlt(alt == null ? "producto" : alt);
        img.setWidth("100%");
        img.setHeight("100%");
        img.getStyle().set("object-fit", "cover");
        img.getElement().setAttribute("loading", "lazy");

        // Fallback
        if (foto == null || foto.isBlank()) {
            img.setSrc("/images/default-product.jpg");
            return img;
        }

        String f = foto.trim();

        // URL absoluta o data URI
        if (f.startsWith("http://") || f.startsWith("https://") || f.startsWith("data:image/")) {
            img.setSrc(f);
            return img;
        }

        // Normaliza (asegura '/')
        if (!f.startsWith("/")) f = "/" + f;

        // Si por error en BD se guardó algo como '/frontend-resources/products/xxx.png',
        // lo reconducimos a la ruta pública correcta (ajusta si tu ruta pública difiere)
        if (f.startsWith("/frontend-resources/products/")) {
            f = f.replace("/frontend-resources/products/", "/product-photos/");
        }
        if (f.startsWith("/frontend-resources/company-logos/")) {
            f = f.replace("/frontend-resources/company-logos/", "/company-logos/");
        }

        // Context path (por si la app no cuelga de '/')
        String ctx = (VaadinService.getCurrentRequest() != null)
                ? VaadinService.getCurrentRequest().getContextPath()
                : "";

        // Cache-buster SOLO para recursos servidos por tu backend
        boolean bust = f.startsWith("/product-photos/") || f.startsWith("/company-logos/");
        String src = ctx + f;
        if (bust) {
            src += (src.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis();
        }

        img.setSrc(src);
        return img;
    }





    private String formatPrice(BigDecimal p) {
        return p == null ? "—" : euro.format(p);
    }

    private void navigate(String route) {
        UI.getCurrent().navigate(route);
    }

    /* ========================= CARRITO ========================= */

    private void addToCart(Producto producto) {
        if (!cajaService.isCajaAbierta()) {
            Notification.show("Caja cerrada: no se pueden realizar pedidos ahora.", 2500,
                            Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        Cliente actual = clienteSesionService.getActual();
        if (actual == null) {
            Notification n = Notification.show(getTranslation("cart.login_required"), 2500,
                    Notification.Position.MIDDLE);
            n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            gestionarCarritoCliente.asegurarCarrito(actual.getId());
            insertarProductoCarrito.meterProductoCarrito(actual.getId(), producto.getId(), 1);

            String msg = producto.getNombre() + getTranslation("cart.added_success");
            Notification n = Notification.show("⭐ " + msg, 1800,
                    Notification.Position.TOP_CENTER);
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } catch (Exception ex) {
            String msg = getTranslation("cart.add_failed") + " " + ex.getMessage();
            Notification n = Notification.show(msg, 3000, Notification.Position.TOP_CENTER);
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

    /* ========================= DATA / PIPELINE ========================= */


    private void reload() {
        List<Producto> productos = gestionarProducto.consultarProductos().stream()
                .filter(p -> Boolean.TRUE.equals(p.isActivo()))
                .filter(p -> p.getTipo() == null || p.getTipo() != ProductoTipo.MENU)
                .toList();

        List<Producto> menus = gestionarMenu.listarMenus().stream()
                .filter(p -> Boolean.TRUE.equals(p.isActivo()))
                .toList();

        allItems = new ArrayList<>();
        allItems.addAll(productos);
        allItems.addAll(menus);

        page = 1;
        applyPipeline();
    }





    private void applyPipeline() {
        String q = Optional.ofNullable(search.getValue()).orElse("").trim().toLowerCase();
        filtered = allItems.stream()
                .filter(p -> q.isBlank()
                        || safe(p.getNombre()).contains(q)
                        || safe(p.getDescripcion()).contains(q))
                .sorted(getComparator())
                .collect(Collectors.toList());

        page = 1;
        renderPage();
    }

    private Comparator<Producto> getComparator() {
        String recommended = getTranslation("toolbar.sort_recommended");
        String priceAsc = getTranslation("toolbar.sort_price_asc");
        String priceDesc = getTranslation("toolbar.sort_price_desc");
        String nameAsc = getTranslation("toolbar.sort_name_asc");

        String v = Optional.ofNullable(sortBy.getValue()).orElse(recommended);

        if (v.equals(priceAsc)) {
            // Precio ascendente
            return Comparator.comparing(
                    p -> Optional.ofNullable(p.getPrecio()).orElse(BigDecimal.ZERO)
            );
        } else if (v.equals(priceDesc)) {
            // Precio descendente
            return Comparator.comparing(
                    (Producto p) -> Optional.ofNullable(p.getPrecio()).orElse(BigDecimal.ZERO)
            ).reversed();
        } else if (v.equals(nameAsc)) {
            // Nombre A-Z
            return Comparator.comparing(p -> safe(p.getNombre()));
        } else {
            // Recomendados: primero nombre, luego precio
            return Comparator.comparing((Producto p) -> safe(p.getNombre()))
                    .thenComparing(p -> Optional.ofNullable(p.getPrecio()).orElse(BigDecimal.ZERO));
        }
    }


    private String safe(String s) {
        return s == null ? "" : s.toLowerCase();
    }

    private void renderPage() {
        grid.removeAll();

        if (filtered.isEmpty()) {
            Div empty = new Div();
            empty.addClassName("client-empty");
            empty.getStyle()
                    .set("padding", "32px")
                    .set("border", "2px dashed var(--lumo-contrast-10pct)")
                    .set("border-radius", "18px")
                    .set("color", "var(--lumo-secondary-text-color)")
                    .set("background", "rgba(255,255,255,.85)")
                    .set("text-align", "center")
                    .set("box-shadow", "0 6px 16px rgba(15,23,42,.12)")
                    .set("grid-column", "1 / -1");

            empty.add(new H4("🤔 " + getTranslation("catalog.empty_title")),
                    new Paragraph(getTranslation("catalog.empty_subtitle")));
            grid.add(empty);
        } else {

            int from = (page - 1) * pageSize;
            int to = Math.min(from + pageSize, filtered.size());
            int currentCount = to-from;
            if (currentCount == 1) {
                grid.getStyle()
                        .set("grid-template-columns", "minmax(0, 540px)")
                        .set("justify-content", "center");
            } else {
                grid.getStyle()
                        .set("grid-template-columns", "repeat(auto-fit, minmax(280px, 1fr))")
                        .remove("justify-content");
            }

            filtered.subList(from, to).forEach(p -> grid.add(productCard(p)));

            counter.setText(
                    getTranslation("toolbar.showing") + " " + (from + 1) + "–" + to + " " +
                            getTranslation("toolbar.showing.of") + " " + filtered.size());

            getChildren()
                    .filter(c -> "pager".equals(c.getElement().getProperty("role")))
                    .findFirst()
                    .ifPresent(pagerComp -> {
                        List<Component> kids = pagerComp.getChildren().collect(Collectors.toList());
                        Button prev = (Button) kids.get(0);
                        Span lbl = (Span) kids.get(1);
                        Button next = (Button) kids.get(2);
                        prev.setEnabled(page > 1);
                        next.setEnabled(page < maxPage());
                        lbl.setText(getTranslation("toolbar.page") + " " + page + " / " + maxPage());
                    });
        }
    }

    private int maxPage() {
        if (filtered.isEmpty()) return 1;
        return (int) Math.ceil((double) filtered.size() / pageSize);
    }

    /* ========================= TEMA OSCURO ========================= */

    private void initThemeToggle() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const stored=localStorage.getItem('client-theme');" +
                        "const prefers=window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light';" +
                        "const theme=stored||prefers;" +
                        "document.documentElement.setAttribute('data-theme', theme);" +
                        "if(theme==='dark'){document.documentElement.setAttribute('theme','dark');}" +
                        "else{document.documentElement.removeAttribute('theme');}"
        ));
    }


    private void toggleTheme() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const el=document.documentElement;" +
                        "const cur=el.getAttribute('data-theme')==='dark'?'light':'dark';" +
                        "el.setAttribute('data-theme',cur); localStorage.setItem('client-theme',cur);" +
                        "if(cur==='dark'){el.setAttribute('theme','dark');}" +
                        "else{el.removeAttribute('theme');}"
        ));
    }

}
