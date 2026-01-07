package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.ProductoTipo;
import es.uca.orderflow.business.services.*;
import es.uca.orderflow.persistence.data.ProductoRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Panel del dueño")
@Route("/backoffice/duennopanel")
@AnonymousAllowed
public class DuennoDashboardView extends VerticalLayout implements BeforeEnterObserver {

    private final GestionarProducto gp;
    private final ProductoAuditService productoAuditService;
    private final ProductoRepository productoRepository;

    private final DuennoSesionService duennoSesionService;
    private final GestionarIngredientes gi;
    private final GestionarMenu gestionarMenu;
    private final OfertaService ofertaService;

    // UI base
    private final Div grid = new Div();
    private final TextField search = new TextField();
    private final ComboBox<String> sortBy = new ComboBox<>();
    private final Span counter = new Span();

    // Menú hamburguesa
    private Dialog navDrawer;

    // Paginación
    private final int pageSize = 3;
    private int page = 1;
    private List<Producto> filtered = new ArrayList<>();
    private List<Producto> allItems = new ArrayList<>();
    private final Set<Long> menuIds = new HashSet<>();
    private final Map<Long, Boolean> isMenuCache = new HashMap<>();

    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

    // fondos
    private static final String LIGHT_BG =
            "radial-gradient(1200px 600px at 50% -200px, rgba(255,255,255,.75), rgba(255,255,255,0))," +
                    "linear-gradient(180deg,#ffe9dd 0%, #fff5ef 40%, #ffffff 100%)";
    private static final String DARK_BG =
            "radial-gradient(1200px 600px at 50% -200px, rgba(16,24,39,.6), rgba(16,24,39,0))," +
                    "linear-gradient(180deg,#0b1220 0%, #0e1629 40%, #0b1220 100%)";

    public DuennoDashboardView(ProductoRepository productoRepository,
                               GestionarProducto gp,
                               DuennoSesionService duennoSesionService,
                               ProductoAuditService productoAuditService,
                               GestionarIngredientes gi,
                               GestionarMenu gestionarMenu,
                               OfertaService ofertaService) {

        this.productoRepository = productoRepository;
        this.duennoSesionService = duennoSesionService;
        this.productoAuditService = productoAuditService;
        this.gestionarMenu = gestionarMenu;
        this.ofertaService = ofertaService;
        this.gp = gp;
        this.gi = gi;

        setId("owner-root");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", LIGHT_BG);

        // Drawer
        buildNavDrawer();
        injectHamburgerCss();

        add(buildTopBar(), buildToolbar(), buildCatalog(), buildPager(), buildFab());
        injectDarkThemeCss();
        initThemeToggle();
        reload();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Duenno actual = duennoSesionService.getActual();
        if (actual == null) event.forwardTo(DuennoLoginView.class);
    }

    /* ========================= TOP BAR ========================= */

    private Component buildTopBar() {
        Div band = new Div();
        band.setId("owner-band");
        band.setWidthFull();
        band.getStyle()
                .set("position", "sticky")
                .set("left", "0")
                .set("right", "0")
                .set("top", "0")
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
        bar.setAlignItems(Alignment.CENTER);
        bar.getStyle().set("padding-left", "12px").set("padding-right", "12px");

        Button burger = new Button(VaadinIcon.MENU.create());
        burger.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        burger.getElement().getClassList().add("owner-burger");
        burger.setAriaLabel("Abrir menú");
        burger.addClickListener(e -> navDrawer.open());

        HorizontalLayout brand = new HorizontalLayout();
        brand.setAlignItems(Alignment.CENTER);

        Icon logo = VaadinIcon.STAR.create();
        logo.setColor("#ff7a59");
        logo.setSize("22px");

        Span brandTxt = new Span("Backoffice");
        brandTxt.getStyle()
                .set("font-weight", "800")
                .set("color", "var(--lumo-body-text-color)")
                .set("font-size", "18px");

        Span hint = new Span("Menú");
        hint.getStyle()
                .set("margin-left", "10px")
                .set("padding", "3px 10px")
                .set("border-radius", "999px")
                .set("font-size", "12px")
                .set("font-weight", "700")
                .set("color", "var(--lumo-secondary-text-color)")
                .set("background", "var(--lumo-contrast-5pct)");

        brand.add(logo, brandTxt, hint);

        Button themeBtn = new Button(VaadinIcon.MOON_O.create());
        themeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeBtn.setAriaLabel("Cambiar tema");
        themeBtn.addClickListener(e -> toggleTheme());

        Button crearMenu = new Button("Crear menú", VaadinIcon.PLUS.create(),
                e -> navigate("/backoffice/menus/crear"));
        crearMenu.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        crearMenu.getStyle().set("border-radius", "999px");

        Button crear = new Button("Crear producto", VaadinIcon.PLUS_CIRCLE.create(),
                e -> navigate("/backoffice/productos/crear"));
        crear.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        crear.getStyle()
                .set("border-radius", "999px")
                .set("background", "linear-gradient(90deg,#2563eb,#1d4ed8)")
                .set("color", "white")
                .set("padding", "6px 14px")
                .set("box-shadow", "0 8px 22px rgba(29,78,216,.25)");

        bar.add(burger, brand);
        bar.expand(brand);
        bar.add(themeBtn, crear, crearMenu);

        band.add(bar);
        return band;
    }

    private void navigate(String route) {
        UI.getCurrent().navigate(route);
    }
    private boolean isMenu(Producto p) {
        if (p == null || p.getId() == null) return false;

        return isMenuCache.computeIfAbsent(p.getId(), id -> {
            // 1) Si viene tipado, perfecto
            if (p.getTipo() == ProductoTipo.MENU) return true;

            // 2) Si tu listarMenus lo identificó
            if (menuIds.contains(id)) return true;

            // 3) Último recurso (fiable): si tiene composición => es menú
            try {
                var comp = gestionarMenu.composicion(id);
                return comp != null && !comp.isEmpty();
            } catch (Exception ex) {
                return false;
            }
        });
    }

    /* ========================= DRAWER (MEJORADO) ========================= */

    private void buildNavDrawer() {
        navDrawer = new Dialog();
        navDrawer.setCloseOnEsc(true);
        navDrawer.setCloseOnOutsideClick(true);

        navDrawer.getElement().getClassList().add("owner-drawer");
        navDrawer.getElement().setAttribute("role", "dialog");
        navDrawer.getElement().setAttribute("aria-label", "Navegación Backoffice");

        Div panel = new Div();
        panel.addClassName("owner-drawer-panel");

        // Header con “avatar” y gradiente
        Div header = new Div();
        header.addClassName("owner-drawer-header");

        Div hero = new Div();
        hero.addClassName("owner-drawer-hero");

        Div avatar = new Div();
        avatar.addClassName("owner-drawer-avatar");
        avatar.add(VaadinIcon.STAR.create());

        Div titles = new Div();
        titles.addClassName("owner-drawer-titlebox");
        H3 t = new H3("Panel");
        t.getStyle().set("margin", "0").set("font-weight", "900");
        Span st = new Span("Accesos rápidos del backoffice");
        st.getStyle().set("display", "block").set("font-size", "12px").set("opacity", "0.88");
        titles.add(t, st);

        Button close = new Button(VaadinIcon.CLOSE_SMALL.create(), e -> navDrawer.close());
        close.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        close.addClassName("owner-drawer-close");
        close.setAriaLabel("Cerrar menú");

        hero.add(avatar, titles);
        header.add(hero, close);

        // Contenido (vertical, con “cards” por sección)
        Div content = new Div();
        content.addClassName("owner-drawer-content");

        content.add(sectionTitle("Gestión"));
        Div g1 = group(
                navItem("Empleados", VaadinIcon.USERS, "/backoffice/empleados"),
                navItem("Dueños", VaadinIcon.USER_STAR, "/backoffice/duennos"),
                navItem("Empresa", VaadinIcon.BUILDING, "/backoffice/empresa"),
                navItem("Menús", VaadinIcon.COFFEE, "/backoffice/menus"),
                navItem("Ofertas", VaadinIcon.LEVEL_UP, "/backoffice/ofertas"),
                navItem("Mesas del restaurante", VaadinIcon.DESKTOP, "/backoffice/crearmesa"),
                navItem("Ingredientes", VaadinIcon.CLIPBOARD_CHECK, "/backoffice/ingredientes/crear")
        );
        content.add(g1);

        content.add(sectionTitle("Avanzado"));
        Div g2 = group(
                navItemPro("Métodos de pago", VaadinIcon.CREDIT_CARD, "/backoffice/pagos"),
                navItemPro("Estadísticas", VaadinIcon.CHART, "/backoffice/estadisticas")
        );
        content.add(g2);

        content.add(sectionTitle("Cuenta"));
        Div g3 = group(
                navItem("Perfil", VaadinIcon.USER, "/backoffice/perfil"),
                navItemDanger("Salir", VaadinIcon.EXIT, "/registro"),
                navItem("Acceso como cliente", VaadinIcon.EXCHANGE, "/registro")
        );
        content.add(g3);

        // Footer CTA
        Div footer = new Div();
        footer.addClassName("owner-drawer-footer");

        Button ctaProd = new Button("Nuevo producto", VaadinIcon.PLUS_CIRCLE.create(), e -> {
            navDrawer.close();
            navigate("/backoffice/productos/crear");
        });
        ctaProd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        ctaProd.addClassName("owner-drawer-cta");

        Button ctaMenu = new Button("Nuevo menú", VaadinIcon.PLUS.create(), e -> {
            navDrawer.close();
            navigate("/backoffice/menus/crear");
        });
        ctaMenu.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        ctaMenu.addClassName("owner-drawer-cta2");

        footer.add(ctaProd, ctaMenu);

        panel.add(header, content, footer);
        navDrawer.add(panel);
    }

    private Div group(Component... items) {
        Div g = new Div();
        g.addClassName("owner-drawer-group");
        g.add(items);
        return g;
    }

    private Component sectionTitle(String text) {
        Div s = new Div();
        s.addClassName("owner-drawer-section");
        s.add(new Span(text));
        return s;
    }

    private Component navItem(String text, VaadinIcon icon, String route) {
        Button b = new Button(text, icon.create());
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        b.addClassName("owner-drawer-item");
        b.getElement().setProperty("title", text);
        b.addClickListener(e -> { navDrawer.close(); navigate(route); });
        return b;
    }

    private Component navItemDanger(String text, VaadinIcon icon, String route) {
        Button b = new Button(text, icon.create());
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        b.addClassName("owner-drawer-item");
        b.addClassName("owner-drawer-item-danger");
        b.getElement().setProperty("title", text);
        b.addClickListener(e -> { navDrawer.close(); navigate(route); });
        return b;
    }

    private Component navItemPro(String text, VaadinIcon icon, String route) {
        Div wrap = new Div();
        wrap.addClassName("owner-drawer-prowrap");

        Button b = new Button(text, icon.create());
        b.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        b.addClassName("owner-drawer-item");
        b.getElement().setProperty("title", text);
        b.addClickListener(e -> { navDrawer.close(); navigate(route); });

        Span pro = new Span("PRO");
        pro.addClassName("owner-drawer-pro");

        wrap.add(b, pro);
        return wrap;
    }

    private void injectHamburgerCss() {
        String css =
                "/* ===== Overlay + animación ===== */" +
                        "vaadin-dialog-overlay::part(overlay){padding:0;}" +
                        "vaadin-dialog-overlay.owner-drawer::part(backdrop){backdrop-filter: blur(12px) saturate(1.12);}" +
                        "vaadin-dialog-overlay.owner-drawer::part(content){" +
                        "  padding:0; border-radius:26px; overflow:hidden;" +
                        "  box-shadow:0 40px 110px rgba(0,0,0,.32);" +
                        "  animation: ownerDrawerIn .18s cubic-bezier(.2,.9,.2,1) both;" +
                        "}" +
                        "@keyframes ownerDrawerIn{from{transform:translateY(10px) scale(.985); opacity:.0}to{transform:none; opacity:1}}" +

                        "/* ===== Panel ===== */" +
                        ".owner-drawer-panel{" +
                        "  width:min(420px, 94vw); height:min(92vh, 760px);" +
                        "  display:flex; flex-direction:column;" +
                        "  background: linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.98));" +
                        "}" +

                        "/* ===== Header “premium” ===== */" +
                        ".owner-drawer-header{" +
                        "  position:relative;" +
                        "  padding:16px 16px 14px;" +
                        "  background:" +
                        "    radial-gradient(900px 280px at 18% -35%, rgba(37,99,235,.28), transparent 58%)," +
                        "    radial-gradient(900px 280px at 96% 12%, rgba(255,122,89,.20), transparent 62%)," +
                        "    linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.78));" +
                        "  border-bottom:1px solid rgba(15,23,42,.10);" +
                        "}" +
                        ".owner-drawer-hero{display:flex; align-items:center; gap:12px;}" +
                        ".owner-drawer-avatar{" +
                        "  width:42px; height:42px; border-radius:16px;" +
                        "  display:flex; align-items:center; justify-content:center;" +
                        "  background: linear-gradient(135deg, rgba(37,99,235,.20), rgba(255,122,89,.18));" +
                        "  box-shadow:0 14px 30px rgba(15,23,42,.10);" +
                        "}" +
                        ".owner-drawer-avatar vaadin-icon{color:#ff7a59;}" +
                        ".owner-drawer-titlebox h3{color: rgba(15,23,42,.94);}"+
                        ".owner-drawer-titlebox span{color: rgba(15,23,42,.62);}"+
                        ".owner-drawer-close{" +
                        "  position:absolute; right:12px; top:12px;" +
                        "  border-radius:14px;" +
                        "  box-shadow:0 10px 22px rgba(15,23,42,.08);" +
                        "  background: rgba(255,255,255,.72);" +
                        "}" +

                        "/* ===== Contenido vertical ===== */" +
                        ".owner-drawer-content{" +
                        "  padding:14px 14px 16px;" +
                        "  overflow:auto; flex:1;" +
                        "  display:flex; flex-direction:column; gap:10px;" +
                        "}" +

                        ".owner-drawer-section{margin:6px 4px 0;}" +
                        ".owner-drawer-section span{" +
                        "  font-size:12px; font-weight:950; letter-spacing:.08em;" +
                        "  color: rgba(15,23,42,.52); text-transform: uppercase;" +
                        "}" +

                        "/* ===== Grupos como “cards” ===== */" +
                        ".owner-drawer-group{" +
                        "  background: rgba(255,255,255,.70);" +
                        "  border:1px solid rgba(15,23,42,.08);" +
                        "  border-radius:18px;" +
                        "  padding:10px;" +
                        "  box-shadow:0 14px 34px rgba(15,23,42,.06);" +
                        "  display:flex; flex-direction:column; gap:8px;" +
                        "}" +

                        "/* ===== Items: list tiles (NO chips) ===== */" +
                        ".owner-drawer-item{" +
                        "  width:100% !important;" +
                        "  justify-content:flex-start;" +
                        "  border-radius:14px;" +
                        "  padding:12px 12px;" +
                        "  font-weight:800;" +
                        "  color: rgba(15,23,42,.90);" +
                        "  background: rgba(37,99,235,.04);" +
                        "  border:1px solid rgba(37,99,235,.10);" +
                        "  box-shadow:0 10px 24px rgba(37,99,235,.05);" +
                        "  transition: transform .12s ease, box-shadow .12s ease, background .12s ease, border-color .12s ease;" +
                        "}" +
                        ".owner-drawer-item vaadin-icon{" +
                        "  padding:8px; border-radius:12px;" +
                        "  background: rgba(255,255,255,.70);" +
                        "  box-shadow: inset 0 0 0 1px rgba(15,23,42,.06);" +
                        "}" +
                        ".owner-drawer-item::after{" +
                        "  content:'›'; margin-left:auto;" +
                        "  font-size:20px; line-height:1;" +
                        "  opacity:.55;" +
                        "}" +
                        ".owner-drawer-item:hover{" +
                        "  transform: translateY(-1px);" +
                        "  background: rgba(37,99,235,.08);" +
                        "  border-color: rgba(37,99,235,.18);" +
                        "  box-shadow: 0 18px 42px rgba(37,99,235,.12);" +
                        "}" +

                        ".owner-drawer-item-danger{" +
                        "  background: rgba(239,68,68,.04);" +
                        "  border:1px solid rgba(239,68,68,.14);" +
                        "}" +
                        ".owner-drawer-item-danger:hover{" +
                        "  background: rgba(239,68,68,.09);" +
                        "  border-color: rgba(239,68,68,.22);" +
                        "  box-shadow: 0 18px 42px rgba(239,68,68,.14);" +
                        "}" +

                        "/* PRO badge */" +
                        ".owner-drawer-prowrap{position:relative;}" +
                        ".owner-drawer-prowrap .owner-drawer-item{padding-right:78px;}" +
                        ".owner-drawer-pro{" +
                        "  position:absolute; right:18px; top:50%; transform:translateY(-50%);" +
                        "  background: linear-gradient(180deg,#fde68a,#fbbf24);" +
                        "  color:#7c2d12;" +
                        "  border-radius:999px; padding:4px 10px;" +
                        "  font-size:11px; font-weight:950;" +
                        "  box-shadow:0 10px 22px rgba(251,191,36,.22);" +
                        "}" +

                        "/* Footer */" +
                        ".owner-drawer-footer{" +
                        "  padding:12px 14px 14px;" +
                        "  border-top:1px solid rgba(15,23,42,.10);" +
                        "  display:flex; gap:10px;" +
                        "  background: linear-gradient(180deg, rgba(255,255,255,.90), rgba(255,255,255,1));" +
                        "}" +
                        ".owner-drawer-cta{" +
                        "  flex:1; border-radius:16px;" +
                        "  background: linear-gradient(90deg,#2563eb,#1d4ed8) !important;" +
                        "  box-shadow: 0 14px 34px rgba(29,78,216,.22);" +
                        "}" +
                        ".owner-drawer-cta2{flex:1; border-radius:16px;}" +

                        "/* ===== Dark mode ===== */" +
                        "[data-theme='dark'] .owner-drawer-panel{" +
                        "  background: linear-gradient(180deg, rgba(17,24,39,.94), rgba(17,24,39,.99));" +
                        "}" +
                        "[data-theme='dark'] .owner-drawer-header{" +
                        "  background:" +
                        "    radial-gradient(900px 280px at 18% -35%, rgba(37,99,235,.26), transparent 58%)," +
                        "    radial-gradient(900px 280px at 96% 12%, rgba(255,122,89,.14), transparent 62%)," +
                        "    linear-gradient(180deg, rgba(17,24,39,.94), rgba(17,24,39,.82));" +
                        "  border-bottom:1px solid rgba(255,255,255,.10);" +
                        "}" +
                        "[data-theme='dark'] .owner-drawer-titlebox h3{color: rgba(229,231,235,.95);}"+
                        "[data-theme='dark'] .owner-drawer-titlebox span{color: rgba(229,231,235,.68);}"+
                        "[data-theme='dark'] .owner-drawer-section span{color: rgba(229,231,235,.55);}"+
                        "[data-theme='dark'] .owner-drawer-group{" +
                        "  background: rgba(17,24,39,.70);" +
                        "  border:1px solid rgba(255,255,255,.10);" +
                        "  box-shadow:0 18px 44px rgba(0,0,0,.28);" +
                        "}" +
                        "[data-theme='dark'] .owner-drawer-item{" +
                        "  color: rgba(229,231,235,.95);" +
                        "  background: rgba(37,99,235,.10);" +
                        "  border:1px solid rgba(37,99,235,.20);" +
                        "  box-shadow:0 16px 40px rgba(0,0,0,.22);" +
                        "}" +
                        "[data-theme='dark'] .owner-drawer-item vaadin-icon{" +
                        "  background: rgba(255,255,255,.06);" +
                        "  box-shadow: inset 0 0 0 1px rgba(255,255,255,.10);" +
                        "}" +
                        "[data-theme='dark'] .owner-drawer-item:hover{background: rgba(37,99,235,.16);}"+
                        "[data-theme='dark'] .owner-drawer-footer{border-top:1px solid rgba(255,255,255,.10);}";

        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('owner-hamburger-css')){" +
                        "const s=document.createElement('style'); s.id='owner-hamburger-css'; s.textContent=$0;" +
                        "document.head.appendChild(s);" +
                        "}", css));
    }

    /* ========================= TOOLBAR ========================= */

    private Component buildToolbar() {
        Div wrap = new Div();
        wrap.getStyle()
                .set("max-width", "1280px")
                .set("margin", "14px auto 0")
                .set("padding", "0 16px");

        HorizontalLayout tb = new HorizontalLayout();
        tb.setWidthFull();
        tb.setPadding(true);
        tb.setAlignItems(Alignment.CENTER);

        search.setPlaceholder("Buscar producto por nombre o descripción…");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setWidth("520px");
        search.addValueChangeListener(e -> getUI().ifPresent(ui ->
                ui.getPage().executeJs("clearTimeout(window._srch); window._srch=setTimeout(()=>$0.$server._f(),200)", search)));
        search.addValueChangeListener(e -> applyPipeline());
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "window.addEventListener('keydown',e=>{if(e.key==='/'&&document.activeElement!==$0.inputElement){e.preventDefault();$0.inputElement.focus();}})",
                search));

        sortBy.setItems("Nombre (A–Z)", "Precio ↑", "Precio ↓");
        sortBy.setValue("Precio ↓");
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

        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const css=`@media(max-width:1100px){.owner-grid{grid-template-columns:repeat(2,1fr)} }" +
                        "@media(max-width:680px){.owner-grid{grid-template-columns:repeat(1,1fr)} }`;" +
                        "if(!document.getElementById('owner-grid-css')){const s=document.createElement('style');s.id='owner-grid-css';s.textContent=css;document.head.appendChild(s);}"
        ));
        grid.getElement().getClassList().add("owner-grid");

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
        Button fab = new Button(VaadinIcon.PLUS.create(), e -> navigate("/backoffice/productos/crear"));
        fab.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        fab.getStyle()
                .set("position", "fixed")
                .set("right", "24px")
                .set("bottom", "24px")
                .set("width", "54px")
                .set("height", "54px")
                .set("border-radius", "50%")
                .set("box-shadow", "0 16px 40px rgba(29,78,216,.35)");
        fab.setAriaLabel("Crear producto");
        return fab;
    }

    /* ========================= DATA ========================= */

    private void reload() {
        List<Producto> productos = gp.consultarSoloProductos();

        List<Producto> menus = gestionarMenu.listarMenus().stream()
                .filter(Producto::isActivo)
                .toList();

        menuIds.clear();
        menus.stream()
                .map(Producto::getId)
                .filter(Objects::nonNull)
                .forEach(menuIds::add);

        productos = productos.stream()
                .filter(p -> p.getId() == null || !menuIds.contains(p.getId()))
                .toList();

        allItems = new ArrayList<>();
        allItems.addAll(productos);
        allItems.addAll(menus);

        isMenuCache.clear();
        page = 1;
        applyPipeline();
    }



    private void applyPipeline() {
        String q = Optional.ofNullable(search.getValue()).orElse("").trim().toLowerCase();

        filtered = allItems.stream()
                .filter(Producto::isActivo)
                .filter(p -> q.isBlank()
                        || safe(p.getNombre()).contains(q)
                        || safe(p.getDescripcion()).contains(q))
                .sorted(getComparator())
                .collect(Collectors.toList());

        page = 1;
        renderPage();
    }


    private Comparator<Producto> getComparator() {
        String v = Optional.ofNullable(sortBy.getValue()).orElse("Nombre (A–Z)");
        switch (v) {
            case "Precio ↑":
                return Comparator.comparing(p -> Optional.ofNullable(p.getPrecio()).orElse(BigDecimal.ZERO));
            case "Precio ↓":
                return Comparator.comparing((Producto p) -> Optional.ofNullable(p.getPrecio()).orElse(BigDecimal.ZERO)).reversed();
            default:
                return Comparator.comparing(p -> safe(p.getNombre()));
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

    /* ========================= CARD ========================= */

    private Component productCard(Producto p) {
        boolean isMenu = isMenu(p);

        OfertaService.PrecioInfo pi = ofertaService.precioParaProducto(p);
        BigDecimal precioMostrar = (pi != null && pi.finalPrice() != null) ? pi.finalPrice() : p.getPrecio();

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
                        .set("border-color", "#dbeafe"));
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

        // Imagen
        Image img = buildImage(p.getFoto(), p.getNombre());
        img.setWidth("100%");
        img.setHeight("100%");
        img.getStyle()
                .set("object-fit", "cover")
                .set("transform", "scale(1)")
                .set("transition", "transform .25s ease")
                .set("z-index", "0");

        imgWrap.getElement().addEventListener("mouseenter",
                e -> img.getStyle().set("transform", "scale(1.035)"));
        imgWrap.getElement().addEventListener("mouseleave",
                e -> img.getStyle().set("transform", "scale(1)"));

        // Overlay brillo
        Div shine = new Div();
        shine.getStyle()
                .set("position", "absolute")
                .set("inset", "0")
                .set("background", "linear-gradient(115deg, rgba(255,255,255,0) 0%, rgba(255,255,255,.35) 45%, rgba(255,255,255,0) 60%)")
                .set("transform", "translateX(-120%)")
                .set("transition", "transform .6s ease")
                .set("z-index", "1");

        imgWrap.getElement().addEventListener("mouseenter",
                e -> shine.getStyle().set("transform", "translateX(120%)"));
        imgWrap.getElement().addEventListener("mouseleave",
                e -> shine.getStyle().set("transform", "translateX(-120%)"));

        // Badge MENÚ
        Span menuBadge = null;
        if (isMenu) {
            menuBadge = new Span("MENÚ");
            menuBadge.getStyle()
                    .set("position", "absolute")
                    .set("right", "10px")
                    .set("top", "10px")
                    .set("padding", "5px 10px")
                    .set("border-radius", "999px")
                    .set("background", "#111827")
                    .set("color", "white")
                    .set("font-weight", "800")
                    .set("font-size", "12px")
                    .set("box-shadow", "0 10px 22px rgba(0,0,0,.22)")
                    .set("z-index", "4");
        }

        // Badge OFERTA
        Span offerBadge = null;
        if (pi != null && pi.hayOferta()) {
            String pctTxt = pi.descuentoPct().stripTrailingZeros().toPlainString();
            offerBadge = new Span("-" + pctTxt + "%");
            offerBadge.getStyle()
                    .set("position", "absolute")
                    .set("left", "10px")
                    .set("top", "10px")
                    .set("padding", "5px 10px")
                    .set("border-radius", "999px")
                    .set("background", "hsl(0,85%,55%)")
                    .set("color", "white")
                    .set("font-weight", "900")
                    .set("font-size", "12px")
                    .set("box-shadow", "0 10px 22px rgba(239,68,68,.28)")
                    .set("z-index", "4");

            if (pi.ofertaNombre() != null && !pi.ofertaNombre().isBlank()) {
                offerBadge.getElement().setProperty("title", pi.ofertaNombre());
            }
        }

        // Precio
        Span price = new Span(formatPrice(precioMostrar));
        price.getStyle()
                .set("position", "absolute")
                .set("left", "10px")
                .set("bottom", "10px")
                .set("padding", "5px 10px")
                .set("border-radius", "10px")
                .set("background", "var(--lumo-base-color)")
                .set("color", "#059669")
                .set("font-weight", "800")
                .set("box-shadow", "0 8px 18px rgba(5,150,105,.22)")
                .set("z-index", "3");

        // ORDEN IMPORTANTE
        imgWrap.removeAll();
        imgWrap.add(img, shine);
        if (menuBadge != null) imgWrap.add(menuBadge);
        if (offerBadge != null) imgWrap.add(offerBadge);
        imgWrap.add(price);

        // Body
        Div body = new Div();
        body.getStyle().set("padding", "12px 14px 8px");

        Span title = new Span(Objects.toString(p.getNombre(), "Producto"));
        title.getStyle()
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "1")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("font-weight", "800")
                .set("color", "var(--lumo-body-text-color)");

        if (pi != null && pi.hayOferta()) {
            Span old = new Span(formatPrice(pi.base()));
            old.getStyle()
                    .set("display", "block")
                    .set("margin-top", "4px")
                    .set("text-decoration", "line-through")
                    .set("opacity", "0.7")
                    .set("color", "var(--lumo-secondary-text-color)");
            body.add(title, old);
        } else {
            body.add(title);
        }

        Span desc = new Span(Objects.toString(p.getDescripcion(), "Sin descripción"));
        desc.getStyle()
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "2")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("color", "var(--lumo-secondary-text-color)");

        body.add(new Paragraph(), desc);

        // Actions
        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.getStyle().set("padding", "0 14px 14px");

        Button edit = new Button("Modificar", VaadinIcon.EDIT.create(), e -> {
            if (isMenu) UI.getCurrent().navigate("/backoffice/menus/editar/" + p.getId());
            else UI.getCurrent().navigate("/backoffice/productos/editar/" + p.getId());
        });
        edit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        edit.getStyle()
                .set("flex", "1")
                .set("min-height", "36px")
                .set("border-radius", "10px")
                .set("background", "linear-gradient(90deg,#2563eb,#1d4ed8)")
                .set("color", "white")
                .set("font-weight", "600")
                .set("box-shadow", "0 6px 16px rgba(37,99,235,.35)");

        Button del = new Button("Eliminar", VaadinIcon.TRASH.create(), e -> confirmDelete(p));
        del.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        del.getStyle().set("flex", "1").set("min-height", "36px").set("border-radius", "10px");

        Button hist = new Button("Histórico", VaadinIcon.CLOCK.create(), e -> showHistory(p.getId()));
        hist.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        actions.add(edit, del, hist);
        actions.setFlexGrow(1, edit, del, hist);

        card.add(imgWrap, body, actions);
        return card;
    }


    private void confirmDelete(Producto p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Eliminar producto");

        dialog.setText("Si el producto está asociado a pedidos o a menús, no se borrará de la base de datos: se archivará y dejará de mostrarse en el catálogo.");

        dialog.setCancelable(true);
        dialog.setConfirmText("Eliminar");
        dialog.setCancelText("Cancelar");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                Long id = p.getId();

                // ÚNICA ruta de borrado para PRODUCTO y MENU:
                // - Si tiene referencias (detalle_pedido o menu_composicion) => archiva
                // - Si no tiene referencias => hard delete
                gp.eliminarProducto(id);

                // Relee para saber si fue borrado o archivado
                Producto tras = productoRepository.findById(id).orElse(null);

                if (tras == null) {
                    Notification.show("Producto eliminado", 2500, Notification.Position.TOP_CENTER);
                } else if (!tras.isActivo()) {
                    Notification n = Notification.show("Producto archivado (tenía referencias)", 3500, Notification.Position.TOP_CENTER);
                    n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
                } else {
                    Notification.show("Operación completada", 2500, Notification.Position.TOP_CENTER);
                }

                reload();

            } catch (Exception ex) {
                Notification n = Notification.show("No se pudo eliminar: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        dialog.open();
    }



    /* ========================= IMAGES ========================= */

    public Image buildImage(String foto, String alt) {
        Image img = new Image();
        img.setAlt(alt == null ? "producto" : alt);
        img.setWidth("100%");
        img.setHeight("100%");
        img.getStyle().set("object-fit", "cover");
        img.getElement().setAttribute("loading", "lazy");

        if (foto == null || foto.isBlank()) {
            img.setSrc("/images/default-product.jpg"); // Ruta por defecto
            System.out.println("DEBUG IMAGEN: Usando imagen por defecto.");
            return img;
        }

        String f = foto.trim();

        // 1) URLs absolutas o data URI
        if (f.startsWith("http://") || f.startsWith("https://") || f.startsWith("data:image/")) {
            img.setSrc(f);
            System.out.println("DEBUG IMAGEN: URL Absoluta/Data URI -> " + f);
            return img;
        }

        // 2) Rutas relativas, usando contexto de la aplicación
        String ctx = (VaadinService.getCurrentRequest() != null)
                ? VaadinService.getCurrentRequest().getContextPath()
                : "";

        if (!f.startsWith("/")) {
            f = "/" + f; // Asegúrate de que la ruta comience con "/"
        }

        // Cache busting (especialmente útil en dev)
        // Esto asegura que la imagen se recarga si el archivo cambia, evitando el caché del navegador.
        String cacheBuster = "v=" + System.currentTimeMillis();
        String finalSrc = ctx + f + (f.contains("?") ? "&" : "?") + cacheBuster;

        img.setSrc(finalSrc);

        // --- DEPURACIÓN ---
        System.out.println("-------------------------------------------------------------------");
        System.out.println("DEBUG IMAGEN: Ruta DB (foto)  -> " + foto);
        System.out.println("DEBUG IMAGEN: Context Path (ctx) -> [" + ctx + "]");
        System.out.println("DEBUG IMAGEN: SRC Final Generado -> " + finalSrc);
        System.out.println("-------------------------------------------------------------------");
        // --- FIN DEPURACIÓN ---

        return img;
    }



    private StreamResource streamIfExists(String classpathPath) {
        String p = classpathPath.startsWith("/") ? classpathPath : "/" + classpathPath;
        if (getClass().getResource(p) == null) return null;
        return new StreamResource(p.substring(p.lastIndexOf('/') + 1), () -> getClass().getResourceAsStream(p));
    }

    private String formatPrice(BigDecimal p) { return p == null ? "—" : euro.format(p); }

    /* ========================= THEME ========================= */

    private void initThemeToggle() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const stored=localStorage.getItem('owner-theme');" +
                        "const prefers=window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light';" +
                        "const theme=stored||prefers;" +
                        "document.documentElement.setAttribute('data-theme', theme);" +
                        "if(theme==='dark'){document.documentElement.setAttribute('theme','dark');}else{document.documentElement.removeAttribute('theme');}" +
                        "const root=document.getElementById('owner-root');" +
                        "const band=document.getElementById('owner-band');" +
                        "if(theme==='dark'){ root.style.background=$0; band.style.background='linear-gradient(180deg, rgba(17,24,39,.82), rgba(17,24,39,.7))'; band.style.borderBottom='1px solid #1f2937'; }" +
                        "else{ root.style.background=$1; band.style.background='linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.86))'; band.style.borderBottom='1px solid #eef2f7'; }",
                DARK_BG, LIGHT_BG));
    }

    private void toggleTheme() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const el=document.documentElement;" +
                        "const cur=el.getAttribute('data-theme')==='dark'?'light':'dark';" +
                        "el.setAttribute('data-theme',cur); localStorage.setItem('owner-theme',cur);" +
                        "if(cur==='dark'){el.setAttribute('theme','dark');}else{el.removeAttribute('theme');}" +
                        "const root=document.getElementById('owner-root');" +
                        "const band=document.getElementById('owner-band');" +
                        "if(cur==='dark'){ root.style.background=$0; band.style.background='linear-gradient(180deg, rgba(17,24,39,.82), rgba(17,24,39,.7))'; band.style.borderBottom='1px solid #1f2937'; }" +
                        "else{ root.style.background=$1; band.style.background='linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.86))'; band.style.borderBottom='1px solid #eef2f7'; }",
                DARK_BG, LIGHT_BG));
    }

    private void injectDarkThemeCss() {
        String css =
                "[data-theme='dark'] .owner-grid > div{background:#111827 !important;border-color:#1f2937 !important;box-shadow:0 10px 26px rgba(0,0,0,.5) !important;}" +
                        "[data-theme='dark'] .owner-grid > div:hover{border-color:#1d4ed8 !important;}" +
                        "[data-theme='dark'] .v-button[theme~='tertiary']{color:#e5e7eb !important;}" +
                        "[data-theme='dark'] #owner-band{background:linear-gradient(180deg, rgba(17,24,39,.82), rgba(17,24,39,.7)) !important; border-bottom:1px solid #1f2937 !important;}";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('owner-dark-css')){const s=document.createElement('style');s.id='owner-dark-css';s.textContent=$0;document.head.appendChild(s);}",
                css));
    }

    /* ========================= HISTORY ========================= */

    private void showHistory(Long productoId) {
        var dlg = new Dialog();
        dlg.setHeaderTitle("Histórico de cambios");

        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(false);
        content.getStyle().set("min-width", "520px");

        content.add(new Paragraph("Últimas modificaciones del producto."));

        UnorderedList ul = new UnorderedList();
        productoAuditService.historial(productoId).forEach(r -> {
            String line = String.format(
                    "[%s] %s – %s | precio=%s, stock=%d",
                    r.action(),
                    r.when(),
                    r.who() == null ? "desconocido" : r.who(),
                    r.precio(),
                    r.stock()
            );
            ul.add(new ListItem(line));
        });

        content.add(ul);
        Button cerrar = new Button("Cerrar", e -> dlg.close());
        cerrar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dlg.add(content);
        dlg.getFooter().add(cerrar);
        dlg.open();
    }
}
