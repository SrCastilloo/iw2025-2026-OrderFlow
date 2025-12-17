package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Empresa;
import es.uca.orderflow.business.services.EmpresaInfoService;

import java.util.Objects;

@PageTitle("Sobre nosotros")
@Route("/cliente/sobre-nosotros")
@AnonymousAllowed
public class SobreNosotrosView extends VerticalLayout implements BeforeEnterObserver {

    private final EmpresaInfoService empresaInfoService;

    public SobreNosotrosView(EmpresaInfoService empresaInfoService) {
        this.empresaInfoService = empresaInfoService;

        setSizeFull();
        setPadding(false);
        setSpacing(false);

        // Fondo general más “premium”
        getStyle().set("background",
                "radial-gradient(1200px 600px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                        "radial-gradient(1000px 500px at 110% 10%, rgba(255,120,90,.35), transparent 60%)," +
                        "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)"
        );


        add(buildTopBar(), buildContent());
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // público (AnonymousAllowed). Si lo quieres protegido, redirige aquí.
    }
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        injectCss(); // aquí SI hay UI, ya se aplica siempre
    }

    /* ========================= TOP BAR ========================= */

    private Component buildTopBar() {
        Div band = new Div();
        band.setWidthFull();
        band.getStyle()
                .set("position", "sticky")
                .set("top", "0")
                .set("z-index", "50")
                .set("backdrop-filter", "blur(14px) saturate(1.25)")
                .set("background", "rgba(255,255,255,0.9)")
                .set("border-bottom", "1px solid rgba(255,120,90,.18)")
                .set("box-shadow", "0 10px 34px rgba(255,120,90,.10)");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.getStyle()
                .set("padding", "10px 16px")
                .set("max-width", "1280px")
                .set("margin", "0 auto");

        Button back = new Button(VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().navigate("/cliente"));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        back.getElement().getClassList().add("about-back");
        back.setAriaLabel("Volver");

        Div titleBox = new Div();
        titleBox.getStyle().set("display", "flex").set("flex-direction", "column");

        H3 title = new H3("Sobre nosotros");
        title.getStyle().set("margin", "0").set("font-weight", "950");

        Span hint = new Span("Conoce la empresa y la información de contacto");
        hint.getStyle().set("opacity", "0.72").set("font-size", "13px").set("font-weight", "600");

        titleBox.add(title, hint);

        bar.add(back, titleBox);
        band.add(bar);
        return band;
    }

    /* ========================= CONTENT ========================= */

    private Component buildContent() {
        Empresa emp = empresaInfoService.obtenerEmpresaActiva();

        String nombre = emp != null ? Objects.toString(emp.getNombreComercial(), "Empresa") : "Empresa";
        String razon  = emp != null ? safeLine(emp.getRazonSocial()) : "—";

        String direccion = emp == null ? null : String.join(" ",
                nullToEmpty(emp.getDireccion1()),
                nullToEmpty(emp.getDireccion2()),
                nullToEmpty(emp.getCodigoPostal()),
                nullToEmpty(emp.getCiudad()),
                nullToEmpty(emp.getProvincia()),
                nullToEmpty(emp.getPais())
        ).trim();

        Div wrap = new Div();
        wrap.getStyle()
                .set("max-width", "1040px")
                .set("margin", "18px auto 28px")
                .set("padding", "0 16px");

        // HERO “impactante”
        Div hero = new Div();
        hero.addClassName("about-hero");

        Div heroInner = new Div();
        heroInner.addClassName("about-hero-inner");

        // Bloques decorativos
        Div blob1 = new Div();
        blob1.addClassNames("about-blob", "about-blob-1");
        Div blob2 = new Div();
        blob2.addClassNames("about-blob", "about-blob-2");
        // Badge
        Span badge = new Span("INFORMACIÓN DE LA EMPRESA");
        badge.addClassName("about-badge");

        // Cabecera: logo + textos
        Div headRow = new Div();
        headRow.addClassName("about-headrow");

        Div logoCard = new Div();
        logoCard.addClassName("about-logo-card");

        Image logo = buildImage(emp != null ? emp.getLogo() : null, "logo");
        logo.addClassName("about-logo");
        logo.setWidth("86px");
        logo.setHeight("86px");

        logoCard.add(logo);

        Div texts = new Div();
        texts.addClassName("about-texts");

        H1 h1 = new H1(nombre);
        h1.addClassName("about-title");

        Paragraph sub = new Paragraph(razon);
        sub.addClassName("about-subtitle");

        texts.add(badge, h1, sub);

        headRow.add(logoCard, texts);

        // “Datos principales” como chips
        Div chips = new Div();
        chips.addClassName("about-chips");
        chips.add(chip(VaadinIcon.ENVELOPE, safe(emp != null ? emp.getCorreo() : null)));
        chips.add(chip(VaadinIcon.PHONE, safe(emp != null ? emp.getTelefono() : null)));
        chips.add(chip(VaadinIcon.GLOBE_WIRE, safe(emp != null ? emp.getNombreWeb() : null)));
        chips.add(chip(VaadinIcon.CREDIT_CARD, safe(emp != null ? emp.getCif() : null)));

        heroInner.add(blob1, blob2, headRow, chips);
        hero.add(heroInner);

        // Grid de secciones (cards)
        Div grid = new Div();
        grid.addClassName("about-grid");

        grid.add(infoCard(
                "Contacto",
                "Cómo localizarte rápidamente",
                VaadinIcon.COMMENTS_O,
                row("Email", emp != null ? emp.getCorreo() : null),
                row("Teléfono", emp != null ? emp.getTelefono() : null),
                row("Web", emp != null ? emp.getNombreWeb() : null)
        ));

        grid.add(infoCard(
                "Identificación",
                "Datos fiscales y corporativos",
                VaadinIcon.BUILDING,
                row("Nombre comercial", emp != null ? emp.getNombreComercial() : null),
                row("Razón social", emp != null ? emp.getRazonSocial() : null),
                row("CIF", emp != null ? emp.getCif() : null)
        ));

        grid.add(infoCard(
                "Dirección",
                "Dónde encontrarnos",
                VaadinIcon.MAP_MARKER,
                row("Dirección", direccion)
        ));

        // CTA inferior
        Div cta = new Div();
        cta.addClassName("about-cta");

        Button volver = new Button("Volver al catálogo", VaadinIcon.ARROW_LEFT.create(),
                e -> UI.getCurrent().navigate("/cliente"));
        volver.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        volver.getStyle()
                .set("border-radius", "999px")
                .set("font-weight", "900")
                .set("padding", "10px 16px")
                .set("background", "linear-gradient(90deg,#2563eb,#1d4ed8)")
                .set("color", "white")
                .set("box-shadow", "0 16px 40px rgba(29,78,216,.25)");

        cta.add(volver);

        wrap.add(hero, grid, cta);
        return wrap;
    }

    /* ========================= UI PARTS ========================= */

    private Component chip(VaadinIcon icon, String text) {
        Div c = new Div();
        c.addClassName("about-chip");

        Icon i = icon.create();
        i.getStyle().set("opacity", "0.9");

        Span t = new Span(text == null || text.isBlank() ? "—" : text);
        t.getStyle().set("font-weight", "800");

        c.add(i, t);
        return c;
    }

    private Component infoCard(String title, String subtitle, VaadinIcon icon, Component... rows) {
        Div box = new Div();
        box.addClassName("about-card");

        Div head = new Div();
        head.addClassName("about-card-head");

        Div ic = new Div();
        ic.addClassName("about-card-icon");
        ic.add(icon.create());

        Div tt = new Div();
        tt.getStyle().set("display", "flex").set("flex-direction", "column").set("gap", "2px");

        H3 h = new H3(title);
        h.getStyle().set("margin", "0").set("font-weight", "950");

        Span s = new Span(subtitle);
        s.getStyle().set("opacity", "0.72").set("font-size", "13px").set("font-weight", "600");

        tt.add(h, s);

        head.add(ic, tt);

        Div body = new Div();
        body.addClassName("about-card-body");
        body.add(rows);

        box.add(head, body);
        return box;
    }

    private Component row(String k, String v) {
        Div r = new Div();
        r.addClassName("about-row");

        Span key = new Span(k);
        key.addClassName("about-row-key");

        Span val = new Span(v == null || v.isBlank() ? "—" : v);
        val.addClassName("about-row-val");

        r.add(key, val);
        return r;
    }

    /* ========================= IMAGE ========================= */

    private Image buildImage(String foto, String alt) {
        Image img = new Image();
        img.setAlt(alt == null ? "imagen" : alt);
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

        if (!f.startsWith("/")) f = "/" + f;

        String ctx = (VaadinService.getCurrentRequest() != null)
                ? VaadinService.getCurrentRequest().getContextPath()
                : "";

        boolean bust = f.startsWith("/company-logos/") || f.startsWith("/product-photos/");
        String src = ctx + f + (bust ? ((f.contains("?") ? "&" : "?") + "t=" + System.currentTimeMillis()) : "");

        img.setSrc(src);
        return img;
    }

    /* ========================= UTIL ========================= */

    private String nullToEmpty(String s) { return s == null ? "" : s; }
    private String safeLine(String s) { return (s == null || s.isBlank()) ? "—" : s; }
    private String safe(String s) { return (s == null || s.isBlank()) ? "—" : s; }

    /* ========================= CSS ========================= */

    private void injectCss() {
        String css =
                ".about-hero{border-radius:24px; overflow:hidden; position:relative; " +
                        "border:1px solid rgba(255,120,90,.18); box-shadow:0 26px 70px rgba(15,23,42,.14);" +
                        "background: linear-gradient(180deg, rgba(255,255,255,.92), rgba(255,255,255,.86));}" +

                        ".about-hero-inner{position:relative; padding:22px; " +
                        "background: radial-gradient(1000px 420px at 8% -30%, rgba(255,120,90,.28), transparent 60%)," +
                        "radial-gradient(900px 380px at 110% 10%, rgba(37,99,235,.22), transparent 60%)," +
                        "linear-gradient(180deg, rgba(255,245,239,1), rgba(255,255,255,1));}" +

                        ".about-blob{position:absolute; filter: blur(28px); opacity:.55; border-radius:999px;}" +
                        ".about-blob-1{width:260px; height:260px; left:-90px; top:-110px; background: rgba(255,120,90,.55);}" +
                        ".about-blob-2{width:320px; height:240px; right:-120px; bottom:-120px; background: rgba(37,99,235,.45);}" +

                        ".about-headrow{display:flex; gap:16px; align-items:center; position:relative;}" +

                        ".about-logo-card{width:110px; height:110px; display:flex; align-items:center; justify-content:center;" +
                        "border-radius:26px; background: rgba(255,255,255,.82); border:1px solid rgba(15,23,42,.08);" +
                        "box-shadow:0 18px 50px rgba(15,23,42,.16);}" +

                        ".about-logo{border-radius:22px; box-shadow: inset 0 0 0 1px rgba(15,23,42,.06);}" +

                        ".about-texts{display:flex; flex-direction:column; gap:6px; min-width:0;}" +
                        ".about-badge{display:inline-flex; width:fit-content; padding:6px 10px; border-radius:999px;" +
                        "background: rgba(37,99,235,.10); border:1px solid rgba(37,99,235,.18);" +
                        "font-weight:950; letter-spacing:.08em; font-size:11px; color: rgba(29,78,216,.95);}" +

                        ".about-title{margin:0; font-weight:980; font-size: clamp(28px, 3.2vw, 42px); line-height:1.05;}" +
                        ".about-subtitle{margin:0; opacity:.75; font-weight:650; max-width: 56ch;}" +

                        ".about-chips{margin-top:14px; display:flex; flex-wrap:wrap; gap:10px; position:relative;}" +
                        ".about-chip{display:inline-flex; align-items:center; gap:8px; padding:9px 12px; border-radius:999px;" +
                        "background: rgba(255,255,255,.82); border:1px solid rgba(15,23,42,.08);" +
                        "box-shadow:0 14px 34px rgba(15,23,42,.10); font-size:13px;}" +

                        ".about-grid{display:grid; grid-template-columns:repeat(3, minmax(0, 1fr)); gap:16px; margin-top:16px;}" +
                        "@media(max-width:980px){.about-grid{grid-template-columns:1fr;}}" +

                        ".about-card{background: rgba(255,255,255,.92); border:1px solid rgba(15,23,42,.08);" +
                        "border-radius:20px; overflow:hidden; box-shadow:0 18px 50px rgba(15,23,42,.12);" +
                        "transition: transform .14s ease, box-shadow .14s ease;}" +
                        ".about-card:hover{transform: translateY(-2px); box-shadow:0 26px 70px rgba(15,23,42,.16);}" +

                        ".about-card-head{display:flex; gap:12px; align-items:center; padding:14px 14px 10px;" +
                        "background: linear-gradient(180deg, rgba(255,255,255,.96), rgba(255,255,255,.90));" +
                        "border-bottom:1px solid rgba(15,23,42,.06);}" +

                        ".about-card-icon{width:44px; height:44px; border-radius:16px; display:flex; align-items:center; justify-content:center;" +
                        "background: linear-gradient(135deg, rgba(255,120,90,.18), rgba(37,99,235,.14));" +
                        "box-shadow:0 14px 34px rgba(15,23,42,.10);} " +
                        ".about-card-icon vaadin-icon{opacity:.9;}" +

                        ".about-card-body{padding:10px 14px 14px; display:flex; flex-direction:column; gap:8px;}" +

                        ".about-row{display:flex; justify-content:space-between; gap:12px; padding:6px 0;" +
                        "border-bottom:1px dashed rgba(15,23,42,.10);} " +
                        ".about-row:last-child{border-bottom:none;}" +
                        ".about-row-key{font-weight:850; opacity:.68; min-width:120px;}" +
                        ".about-row-val{font-weight:800; text-align:right; word-break:break-word;}" +

                        ".about-cta{display:flex; justify-content:flex-end; margin-top:14px; padding: 0 2px;}" +
                        ".about-back{border-radius:14px;}";

        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('about-css')){" +
                        "const s=document.createElement('style'); s.id='about-css'; s.textContent=$0; document.head.appendChild(s);" +
                        "}", css
        ));
    }
}
