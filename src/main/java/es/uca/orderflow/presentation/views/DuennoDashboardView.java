package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.persistence.data.ProductoRepository;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Panel del dueño")
@Route("/backoffice/duennopanel")
@AnonymousAllowed
public class DuennoDashboardView extends VerticalLayout {

    private final ProductoRepository productoRepository;

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

    public DuennoDashboardView(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;

        setId("owner-root");
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        getStyle().set("background", LIGHT_BG);

        add(buildTopBar(), buildToolbar(), buildCatalog(), buildPager(), buildFab());
        injectDarkThemeCss(); // reglas dark globales mínimas
        initThemeToggle();    // autodetección + toggle (activa Lumo dark)
        reload();
    }

    /* ========================= CABECERA FULL-WIDTH ========================= */

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

        HorizontalLayout brand = new HorizontalLayout();
        brand.setAlignItems(Alignment.CENTER);
        Icon logo = VaadinIcon.STAR.create();
        logo.setColor("#ff7a59");
        logo.setSize("22px");
        Span brandTxt = new Span("Backoffice");
        brandTxt.getStyle().set("font-weight", "800").set("color", "var(--lumo-body-text-color)").set("font-size", "18px");
        brand.add(logo, brandTxt);

        HorizontalLayout menu = new HorizontalLayout();
        menu.setSpacing(true);
        menu.setPadding(false);
        menu.add(
                navChip("Empleados", VaadinIcon.USERS, () -> navigate("/backoffice/empleados")),
                navChip("Dueños", VaadinIcon.USER_STAR, () -> navigate("/backoffice/duennos")),
                navChip("Empresa", VaadinIcon.BUILDING, () -> navigate("/backoffice/empresa")),
                proChip("Métodos de pago", VaadinIcon.CREDIT_CARD, () -> navigate("/backoffice/pagos")),
                proChip("Estadísticas", VaadinIcon.CHART, () -> navigate("/backoffice/estadisticas")),
                navChip("Perfil", VaadinIcon.USER, () -> navigate("/backoffice/perfil")),
                navChip("Salir", VaadinIcon.EXIT, () -> navigate("/registro")),
                navChip("Acceso como cliente", VaadinIcon.EXCHANGE, () -> navigate("/registro"))




        );

        Button themeBtn = new Button(VaadinIcon.MOON_O.create());
        themeBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        themeBtn.setAriaLabel("Cambiar tema");
        themeBtn.addClickListener(e -> toggleTheme());

        Button crear = new Button("Crear producto", VaadinIcon.PLUS_CIRCLE.create(),
                e -> navigate("/backoffice/productos/crear"));
        crear.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        crear.getStyle()
                .set("border-radius", "999px")
                .set("background", "linear-gradient(90deg,#2563eb,#1d4ed8)")
                .set("color", "white")
                .set("padding", "6px 14px")
                .set("box-shadow", "0 8px 22px rgba(29,78,216,.25)");

        bar.add(brand);
        bar.expand(brand);
        bar.add(menu, themeBtn, crear);

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
            b.getStyle().set("box-shadow", "inset 0 -2px 0 0 #1d4ed8");
            b.getStyle().set("transform", "translateY(-1px)");
        });
        b.getElement().addEventListener("mouseleave", e -> {
            b.getStyle().remove("box-shadow");
            b.getStyle().set("transform", "none");
        });
        b.addClickListener(e -> action.run());
        return b;
    }

    private Component proChip(String text, VaadinIcon icon, Runnable action) {
        Button b = navChip(text, icon, action);
        Span pro = new Span("PRO");
        pro.getStyle()
                .set("background", "#fde68a")
                .set("color", "#92400e")
                .set("border-radius", "999px")
                .set("padding", "2px 8px")
                .set("font-size", "11px")
                .set("margin-left", "6px");
        HorizontalLayout wrap = new HorizontalLayout(b, pro);
        wrap.setAlignItems(Alignment.CENTER);
        wrap.setSpacing(false);
        return wrap;
    }

    private void navigate(String route) { UI.getCurrent().navigate(route); }

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
                "window.addEventListener('keydown',e=>{if(e.key==='/'&&document.activeElement!==$0.inputElement){e.preventDefault();$0.inputElement.focus();}})", search));

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
                        "if(!document.getElementById('owner-grid-css')){const s=document.createElement('style');s.id='owner-grid-css';s.textContent=css;document.head.appendChild(s);}"));
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
        filtered = productoRepository.findAll();
        page = 1;
        applyPipeline();
    }

    private void applyPipeline() {
        String q = Optional.ofNullable(search.getValue()).orElse("").trim().toLowerCase();
        filtered = productoRepository.findAll().stream()
                .filter(p -> q.isBlank() || safe(p.getNombre()).contains(q) || safe(p.getDescripcion()).contains(q))
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

        Image img = buildImage(p.getFoto(), p.getNombre());
        img.setWidth("100%");
        img.setHeight("100%");
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
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "1")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("font-weight", "800")
                .set("color", "var(--lumo-body-text-color)");
        Span desc = new Span(Objects.toString(p.getDescripcion(), "Sin descripción"));
        desc.getStyle()
                .set("display", "-webkit-box")
                .set("-webkit-line-clamp", "2")
                .set("-webkit-box-orient", "vertical")
                .set("overflow", "hidden")
                .set("color", "var(--lumo-secondary-text-color)");

        body.add(title, new Paragraph(), desc);

        HorizontalLayout actions = new HorizontalLayout();
        actions.setWidthFull();
        actions.setPadding(false);
        actions.setSpacing(true);
        actions.getStyle().set("padding", "0 14px 14px");

        Button edit = new Button("Modificar", VaadinIcon.EDIT.create(),
                e -> UI.getCurrent().navigate("/backoffice/productos/editar/" + p.getId()));
        edit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        edit.getStyle()
                .set("flex", "1")
                .set("min-height", "36px")
                .set("border-radius", "10px");

        Button del = new Button("Eliminar", VaadinIcon.TRASH.create(), e -> confirmDelete(p));
        del.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        del.getStyle()
                .set("flex", "1")
                .set("min-height", "36px")
                .set("border-radius", "10px");

        actions.add(edit, del);
        actions.setFlexGrow(1, edit, del);

        card.add(imgWrap, body, actions);
        return card;
    }

    private void confirmDelete(Producto p) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Eliminar producto");
        dialog.setText("¿Seguro que deseas eliminar \"" + p.getNombre() + "\"? Esta acción no se puede deshacer.");
        dialog.setCancelable(true);
        dialog.setConfirmText("Eliminar");
        dialog.setCancelText("Cancelar");
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(e -> {
            try {
                productoRepository.deleteById(p.getId());
                reload();
                Notification.show("Producto eliminado", 2500, Notification.Position.TOP_CENTER);
            } catch (Exception ex) {
                Notification n = Notification.show("No se pudo eliminar: " + ex.getMessage(), 4000, Notification.Position.TOP_CENTER);
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        dialog.open();
    }

    /* ========================= IMÁGENES & FORMAT ========================= */

    private Image buildImage(String foto, String alt) {
        Image img = new Image();
        img.setAlt(alt == null ? "producto" : alt);
        img.setWidth("100%");
        img.setHeight("100%");
        img.getStyle().set("object-fit", "cover");
        img.getElement().setAttribute("loading", "lazy");

        if (foto == null || foto.isBlank()) { img.setSrc(""); return img; }

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

    /* ========================= DARK THEME ========================= */

    private void initThemeToggle() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const stored=localStorage.getItem('owner-theme');" +
                        "const prefers=window.matchMedia('(prefers-color-scheme: dark)').matches?'dark':'light';" +
                        "const theme=stored||prefers;" +
                        "document.documentElement.setAttribute('data-theme', theme);" +
                        // activar Lumo dark para TODOS los componentes
                        "if(theme==='dark'){document.documentElement.setAttribute('theme','dark');}else{document.documentElement.removeAttribute('theme');}" +
                        // aplicar fondos root + cabecera
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
                        // Lumo dark ON/OFF
                        "if(cur==='dark'){el.setAttribute('theme','dark');}else{el.removeAttribute('theme');}" +
                        // fondos
                        "const root=document.getElementById('owner-root');" +
                        "const band=document.getElementById('owner-band');" +
                        "if(cur==='dark'){ root.style.background=$0; band.style.background='linear-gradient(180deg, rgba(17,24,39,.82), rgba(17,24,39,.7))'; band.style.borderBottom='1px solid #1f2937'; }" +
                        "else{ root.style.background=$1; band.style.background='linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.86))'; band.style.borderBottom='1px solid #eef2f7'; }",
                DARK_BG, LIGHT_BG));
    }

    /** overrides mínimos para oscuro */
    private void injectDarkThemeCss() {
        String css =
                // grid/cards
                "[data-theme='dark'] .owner-grid > div{background:#111827 !important;border-color:#1f2937 !important;box-shadow:0 10px 26px rgba(0,0,0,.5) !important;}" +
                        "[data-theme='dark'] .owner-grid > div:hover{border-color:#1d4ed8 !important;}" +
                        // textos y chips
                        "[data-theme='dark'] .v-button[theme~='tertiary']{color:#e5e7eb !important;}" +
                        // cabecera banda (por si algún estilo inline se impone)
                        "[data-theme='dark'] #owner-band{background:linear-gradient(180deg, rgba(17,24,39,.82), rgba(17,24,39,.7)) !important; border-bottom:1px solid #1f2937 !important;}";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('owner-dark-css')){const s=document.createElement('style');s.id='owner-dark-css';s.textContent=$0;document.head.appendChild(s);}", css));
    }
}
