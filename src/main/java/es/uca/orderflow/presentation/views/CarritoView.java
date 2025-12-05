package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.Unit;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.i18n.I18NProvider; // <-- Importación necesaria
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.services.*;
import es.uca.orderflow.business.entities.Cliente;

import org.springframework.beans.factory.annotation.Autowired;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

@PageTitle("Mi carrito") // Se actualiza dinámicamente
@Route("/cliente/carrito")
@AnonymousAllowed
public class CarritoView extends VerticalLayout implements BeforeEnterObserver {

    private final ClienteSesionService clienteSesionService;
    private final CarritoQueryService carritoQueryService;
    private final QuitarProductoCarrito quitarProductoCarrito;
    private final CheckoutService checkoutService;
    private final MetodoPagoConfigService metodoPagoConfigService;
    private final GestionarCarritoCliente gestionarCarritoCliente;
    private final I18NProvider i18nProvider; // <-- Inyectado

    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

    // UI
    private final Div list = new Div();
    private final Span totalLbl = new Span();
    private final TextField direccionEnvio;
    private final ComboBox<PaymentMethod> metodoPago;
    private final Checkbox confirm;
    private final Button pagarBtn;
    private final Button volverBtn;

    @Autowired
    public CarritoView(ClienteSesionService clienteSesionService,
                       CarritoQueryService carritoQueryService,
                       QuitarProductoCarrito quitarProductoCarrito,
                       CheckoutService checkoutService,
                       MetodoPagoConfigService metodoPagoConfigService,
                       GestionarCarritoCliente gestionarCarritoCliente,
                       I18NProvider i18nProvider) {
        this.clienteSesionService = clienteSesionService;
        this.carritoQueryService = carritoQueryService;
        this.quitarProductoCarrito = quitarProductoCarrito;
        this.checkoutService = checkoutService;
        this.metodoPagoConfigService = metodoPagoConfigService;
        this.gestionarCarritoCliente = gestionarCarritoCliente;
        this.i18nProvider = i18nProvider; // Inicialización de I18NProvider

        // Inicialización de componentes con textos traducidos
        direccionEnvio = new TextField(getTranslation("cart.shipping_address"));
        metodoPago = new ComboBox<>(getTranslation("cart.payment_method"));
        confirm = new Checkbox(getTranslation("cart.confirm_data"));
        pagarBtn = new Button(getTranslation("button.process_payment"), VaadinIcon.CREDIT_CARD.create());
        volverBtn = new Button(getTranslation("button.back"), VaadinIcon.ARROW_LEFT.create());

        // Establecer el título de la página traducido
        setPageTitle(getTranslation("view.cart.title"));

        setId("cart-root");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setHeightFull();

        // Fondo base BLANCO sin “hueco” abajo
        getStyle().set("background", "#ffffff");

        injectGlobalResetCss();
        injectCartCss();
        injectCartJs(); // micro-interacciones (parallax/reveal/tilt)

        add(buildTopBar(), buildContent());

        // Responsive grid
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const css=`@media(max-width:980px){.cart-grid{grid-template-columns:1fr} }`;" +
                        "if(!document.getElementById('cart-css')){const s=document.createElement('style');s.id='cart-css';s.textContent=css;document.head.appendChild(s);}"));

        reload();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // obtenemos el cliente desde tu servicio de sesión
        Cliente clienteActivo = clienteSesionService.getActual();

        if (clienteActivo == null) {
            // nadie logueado, mandamos al login
            event.forwardTo(LoginView.class);
        }
    }

    private Component buildTopBar() {
        Div band = new Div();
        band.setWidthFull();
        band.getStyle()
                .set("position","sticky").set("top","0").set("z-index","60")
                .set("backdrop-filter","blur(12px) saturate(1.05)")
                .set("background","linear-gradient(180deg, rgba(255,255,255,.94), rgba(255,255,255,.86))")
                .set("border-bottom","1px solid #eef2f7").set("box-shadow","0 6px 22px rgba(15,23,42,.08)");

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.setPadding(true);

        // Indicador de pasos (visual)
        Div steps = new Div();
        steps.addClassName("cart-steps");
        // TRADUCCIÓN: Carrito de compra
        steps.add(new Span(getTranslation("cart.step_cart")), new Span(getTranslation("cart.step_of")), new Span(getTranslation("cart.step_purchase")));

        H2 title = new H2(getTranslation("view.cart.title")); // <-- TRADUCCIÓN
        title.getStyle().set("margin","0").set("font-weight","900");

        volverBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volverBtn.addClassName("btn-ghost");
        volverBtn.addClickListener(e -> UI.getCurrent().navigate("/cliente"));

        bar.add(title, steps);
        bar.expand(title);
        bar.add(volverBtn);

        band.add(bar);
        return band;
    }

    private Component buildContent() {
        Div outer = new Div();
        outer.addClassName("cart-shell");
        outer.getStyle()
                .set("max-width","1200px")
                .set("margin","20px auto 28px")
                .set("padding","0 16px");

        // grid 2 columnas: izquierda (items) / derecha (resumen)
        Div grid = new Div();
        grid.getElement().getClassList().add("cart-grid");
        grid.getStyle().set("display","grid")
                .set("grid-template-columns","2fr 1fr")
                .set("gap","20px");

        /* ===== Columna izquierda ===== */
        Div left = new Div();
        left.addClassNames("cart-card", "reveal");

        Div header = new Div();
        header.getStyle().set("padding","14px 16px").set("border-bottom","1px solid var(--cart-border)");
        H3 h = new H3(getTranslation("cart.products_added")); // <-- TRADUCCIÓN
        h.getStyle().set("margin","0");
        Div ribbon = new Div(new Span(getTranslation("cart.ribbon_selected"))); // <-- TRADUCCIÓN
        ribbon.addClassName("ribbon");
        header.add(h, ribbon);

        // Banner/notice visual (opcional, solo UI)

        list.getStyle()
                .set("padding","12px 12px 16px")
                .set("display","flex")
                .set("flex-direction","column")
                .set("gap","12px");

        Scroller sc = new Scroller(list);
        sc.addClassName("cart-scroll");
        left.add(header, sc);

        /* ===== Columna derecha: resumen/pago ===== */
        Div rightWrap = new Div(); // contenedor con borde conic animado
        rightWrap.addClassNames("card-frame", "reveal");

        Div right = new Div();
        right.addClassNames("cart-card", "cart-summary", "summary-brutal");
        right.getStyle().set("padding","18px 16px 16px");

        Div summaryHeader = new Div();
        summaryHeader.addClassName("summary-head");
        summaryHeader.add(new Span(getTranslation("cart.summary_title"))); // <-- TRADUCCIÓN

        Span totalTitle = new Span(getTranslation("cart.summary_total")); // <-- TRADUCCIÓN
        totalTitle.getStyle().set("font-weight","900").set("letter-spacing",".2px");
        totalLbl.getElement().getClassList().add("total-ink");

        HorizontalLayout tot = new HorizontalLayout(totalTitle, totalLbl);
        tot.setWidthFull();
        tot.setJustifyContentMode(JustifyContentMode.BETWEEN);

        // Perks visuales (TRADUCCIÓN)
        Div perks = new Div();
        perks.addClassName("perks");
        perks.add(
                iconText("⚡", getTranslation("cart.perk.shipping_time")),
                iconText("🔒", getTranslation("cart.perk.secure_payment")),
                iconText("↩️", getTranslation("cart.perk.no_returns"))
        );

        // Dirección de envío
        direccionEnvio.setWidthFull();
        direccionEnvio.setPlaceholder(getTranslation("cart.address_placeholder")); // <-- TRADUCCIÓN
        var actual = clienteSesionService.getActual();
        if (actual != null && actual.getDireccion() != null) {
            direccionEnvio.setValue(Objects.toString(actual.getDireccion(), ""));
        }

        // Método de pago (extensible)
        metodoPago.setWidthFull();
        metodoPago.setItems(metodoPagoConfigService.getDisponibles());
        // Reemplaza itemLabelGenerator por la traducción
        metodoPago.setItemLabelGenerator(this::getTranslatedPayment);

        metodoPago.setValue(metodoPagoConfigService.getPredeterminado());

        confirm.getStyle().set("margin-top","6px");

        pagarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        // Botón de pago sin ripple ni tilt (estilo sobrio)
        pagarBtn.addClassNames("cta-primary", "btn-solid");
        pagarBtn.setWidthFull();
        pagarBtn.addClickListener(e -> onPay());

        // Barra de confianza


        right.add(summaryHeader,
                new Div(){{
                    addClassName("summary-stats");
                    add(blockStat(getTranslation("cart.stat.items"), "cart-count", " "), // <-- TRADUCCIÓN
                            blockStat(getTranslation("cart.stat.shipping"), "cart-ship", getTranslation("cart.stat.shipping.value")), // <-- TRADUCCIÓN
                            blockStat(getTranslation("cart.stat.tax"), "cart-tax ", getTranslation("cart.stat.tax.value"))); // <-- TRADUCCIÓN
                }},
                new Hr(),
                tot,
                new Hr(),
                direccionEnvio, metodoPago, confirm,
                pagarBtn,
                new Hr(),
                perks);

        rightWrap.add(right);

        grid.add(left, rightWrap);
        outer.add(grid);
        return outer;
    }

    // Bloque mini-estadística (sólo visual)
    private Component blockStat(String label, String id, String value) {
        Div b = new Div();
        b.addClassName("stat");
        Span l = new Span(label);
        l.addClassName("stat-l");
        Span v = new Span(value);
        v.addClassName("stat-v");
        v.setId(id);
        b.add(l, v);
        return b;
    }

    private Component iconText(String icon, String text) {
        Div d = new Div();
        d.addClassName("perk");
        d.add(new Span(icon), new Span(text));
        return d;
    }

    private void reload() {
        refreshPaymentMethods();
        list.removeAll();
        var actual = clienteSesionService.getActual();
        if (actual == null) {
            list.add(new Paragraph(getTranslation("cart.not_logged_in"))); // <-- TRADUCCIÓN
            totalLbl.setText(euro.format(0));
            return;
        }

        gestionarCarritoCliente.asegurarCarrito(actual.getId());
        var resumen = carritoQueryService.obtenerResumen(actual.getId());

        if (resumen.items().isEmpty()) {
            Div empty = new Div(new H4(getTranslation("cart.empty.title")), // <-- TRADUCCIÓN
                    new Paragraph(getTranslation("cart.empty.subtitle"))); // <-- TRADUCCIÓN
            empty.getStyle().set("padding","22px").set("text-align","center")
                    .set("color","var(--lumo-secondary-text-color)");
            list.add(empty);
        } else {
            resumen.items().forEach(item -> {
                Component row = itemRow(item);
                row.getElement().getClassList().add("reveal");
                list.add(row);
            });
        }
        totalLbl.setText(euro.format(resumen.total()));

        // Actualiza stats visuales (sin lógica adicional; sólo texto)
        UI.getCurrent().getPage().executeJs("""
          const €=n=>new Intl.NumberFormat('es-ES',{style:'currency',currency:'EUR'}).format(n);
          const count=$0, ship=$1, tax=$2;
          const c=document.getElementById('cart-count'); if(c) c.textContent=count;
          const s=document.getElementById('cart-ship'); if(s) s.textContent=ship;
          const t=document.getElementById('cart-tax'); if(t) t.textContent=tax;
        """, resumen.items().size(), getTranslation("cart.stat.shipping.value"), getTranslation("cart.stat.tax.value"));
    }

    private Component itemRow(LineaCarritoDTO item) {
        Div row = new Div();
        // Deja tilt en los ítems (queda bien). Si no lo quieres: usa solo "cart-item".
        row.addClassNames("cart-item", "tilt");

        Image img = buildImage(item.foto(), item.nombre());
        img.setWidth(86, Unit.PIXELS);
        img.setHeight(66, Unit.PIXELS);
        img.getStyle().set("object-fit","cover").set("border-radius","12px");

        Div body = new Div();
        body.addClassName("item-body");
        Span name = new Span(item.nombre());
        name.getStyle().set("font-weight","900");
        Div chips = new Div();
        chips.addClassName("chips");
        // TRADUCCIÓN: Cantidad: X
        Span qty = new Span(getTranslation("cart.item.quantity", item.cantidad()));
        qty.getStyle().set("color","var(--lumo-secondary-text-color)");
        // TRADUCCIÓN: Unitario: €X
        Span unit = new Span(getTranslation("cart.item.unit_price", euro.format(item.precioUnitario())));
        unit.getStyle().set("color","#334155");

        body.add(name, chips, qty, unit);

        Div right = new Div();
        right.addClassName("item-right");
        Span sub = new Span(euro.format(item.subtotal()));
        sub.getStyle().set("font-weight","900").set("color","#059669");
        sub.addClassName("price-badge");

        // TRADUCCIÓN: Eliminar
        Button del = new Button(getTranslation("button.delete"), VaadinIcon.TRASH.create(), e -> {
            var actual = clienteSesionService.getActual();
            if (actual == null) return;
            try {
                quitarProductoCarrito.eliminarProducto(actual.getId(), item.productoId());
                // TRADUCCIÓN
                Notification.show(getTranslation("notification.product_removed"), 1800, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                reload();
            } catch (Exception ex) {
                // TRADUCCIÓN
                Notification.show(getTranslation("notification.removal_failed", ex.getMessage()), 2800, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        del.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        del.addClassNames("btn-ghost", "btn-dangerish");
        right.add(sub, del);

        row.add(img, body, right);
        return row;
    }

    private void onPay() {
        var actual = clienteSesionService.getActual();
        if (actual == null) {
            // TRADUCCIÓN
            Notification.show(getTranslation("notification.login_to_pay")).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (!confirm.getValue()) {
            // TRADUCCIÓN
            Notification.show(getTranslation("notification.confirm_data")).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }
        if (direccionEnvio.getValue() == null || direccionEnvio.getValue().isBlank()) {
            // TRADUCCIÓN
            Notification.show(getTranslation("notification.enter_address")).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        pagarBtn.setEnabled(false);
        pagarBtn.setText(getTranslation("button.processing")); // TRADUCCIÓN: Procesando...
        pagarBtn.setIcon(VaadinIcon.SPINNER.create());

        try {
            var req = new PaymentRequest()
                    .withAddress(direccionEnvio.getValue())
                    .withOpaqueToken("simulado-token-externo");
            var result = checkoutService.checkout(actual.getId(), metodoPago.getValue(), req);

            Dialog ok = new Dialog();
            ok.setHeaderTitle(getTranslation("dialog.payment_success.title")); // TRADUCCIÓN
            ok.add(new Paragraph(getTranslation("dialog.payment_success.message", result.orderId()))); // TRADUCCIÓN

            Button goOrders = new Button(getTranslation("button.view_orders"), e -> { // TRADUCCIÓN
                ok.close();
                UI.getCurrent().navigate("/cliente/pedidos");
            });
            ok.getFooter().add(goOrders);
            ok.open();

        } catch (Exception ex) {
            // TRADUCCIÓN
            Notification.show(getTranslation("notification.payment_error", ex.getMessage()), 3500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            pagarBtn.setEnabled(true);
            pagarBtn.setText(getTranslation("button.process_payment")); // TRADUCCIÓN: Procesar pago
            pagarBtn.setIcon(VaadinIcon.CREDIT_CARD.create());
            reload();
        }
    }


    /** Helper para obtener traducción de PaymentMethod */
    private String getTranslatedPayment(PaymentMethod pm) {
        if (pm == null) return "";
        // Clave: payment.[nombre_enum_en_minusculas]
        return getTranslation("payment." + pm.name().toLowerCase());
    }

    /** Método para actualizar el PageTitle */
    private void setPageTitle(String title) {
        UI.getCurrent().getPage().setTitle(title);
    }

    /* ===== util img ===== */
    private Image buildImage(String foto, String alt) {
        Image img = new Image();
        img.setAlt(alt == null ? "producto" : alt);
        img.setWidth("100%");
        img.setHeight("100%");
        img.getStyle().set("object-fit","cover");
        img.getElement().setAttribute("loading","lazy");

        if (foto == null || foto.isBlank()) { img.setSrc("/images/default-product.jpg"); return img; }
        String f = foto.trim();
        if (f.startsWith("http://") || f.startsWith("https://") || f.startsWith("data:image/")) { img.setSrc(f); return img; }

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
    // dentro de CarritoView

    private void refreshPaymentMethods() {
        var disponibles = metodoPagoConfigService.getDisponibles();
        metodoPago.setItems(disponibles);

        // Etiquetas traducidas
        metodoPago.setItemLabelGenerator(this::getTranslatedPayment);

        // Evita NPE y fuerza un valor válido
        PaymentMethod current = metodoPago.getValue();
        if (current == null || !disponibles.contains(current)) {
            metodoPago.setValue(metodoPagoConfigService.getPredeterminado());
        }
    }


    /** Reseteo global: fondo blanco y sin “hueco” */
    private void injectGlobalResetCss() {
        String css = """
:root{
  --lumo-base-color:#ffffff;
  --lumo-primary-color:#2563eb;
  --lumo-body-text-color:#0f172a;
}
html, body{
  background:#ffffff !important;
  min-height:100%;
}
vaadin-app-layout, #outlet, #outlet > *{
  background:#ffffff !important;
}
#cart-root{
  min-height:100vh;
  background:#ffffff;
  position:relative;
  overflow-x:hidden;
}
""";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('cart-reset-css')){const s=document.createElement('style');s.id='cart-reset-css';s.textContent=$0;document.head.appendChild(s);}else{document.getElementById('cart-reset-css').textContent=$0;}", css));
    }

    /** CSS: brutal + interacciones (botón de pago sobrio) */
    private void injectCartCss() {
        String css = """
#cart-root{
  --cart-accent:#2563eb;
  --cart-accent-2:#10b981;
  --cart-ink:#0f172a;
  --cart-ink-2:#334155;
  --cart-card:rgba(255,255,255,.92);
  --cart-border:rgba(15,23,42,.10);
  --cart-shadow:0 28px 68px rgba(15,23,42,.16);

  position:relative;
  isolation:isolate;
  /* pattern cuadriculado + líneas finas + base blanca */
  background:
    radial-gradient(rgba(0,0,0,.04) 1px, transparent 1px) 0 0/14px 14px,
    repeating-linear-gradient(90deg, rgba(0,0,0,.025) 0 1px, transparent 1px 14px),
    #ffffff;
}
/* Blobs superiores */
#cart-root::before{
  content:"";
  position:absolute; inset:-8% -20% auto -20%;
  height:44vh; z-index:-1;
  background:
    radial-gradient(720px 360px at 12% 8%, rgba(255,182,134,.30), transparent 60%),
    radial-gradient(780px 380px at 88% 6%, rgba(59,130,246,.22), transparent 60%),
    radial-gradient(640px 360px at 50% -8%, rgba(16,185,129,.18), transparent 60%);
  filter:blur(22px);
}
/* Grain sutil */
#cart-root::after{
  content:""; position:absolute; inset:0; z-index:-1;
  pointer-events:none; opacity:.25;
  background-image:url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='120' height='120'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/><feColorMatrix type='saturate' values='0'/><feComponentTransfer><feFuncA type='table' tableValues='0 0 0.03 0'/></feComponentTransfer></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>");
  mix-blend-mode:multiply;
}
.cart-shell{ position:relative; z-index:1; }

/* Paso/título */
h2::after{
  content:""; display:block; margin-top:6px; width:72px; height:4px; border-radius:999px;
  background:linear-gradient(90deg, #60a5fa, #34d399);
  box-shadow:0 6px 14px rgba(52,211,153,.28);
}
.cart-steps{ display:flex; gap:10px; align-items:center; margin-left:12px; }
.cart-steps span{
  font-weight:800; font-size:.85rem; color:#0f172a; opacity:.85;
  background:linear-gradient(180deg,#fff,#f7fafc);
  border:1px solid #e5e7eb; border-radius:999px; padding:6px 10px;
  box-shadow:0 10px 20px rgba(15,23,42,.06);
}
.cart-steps span:nth-child(1){ outline:2px solid #c7d2fe; }

/* Notice/banner */
.notice{
  margin:10px 12px 0; padding:8px 12px; border-radius:12px;
  border:1px dashed #e2e8f0;
  background:linear-gradient(180deg,#ffffff,#f8fafc);
  font-size:.9rem; display:flex; gap:8px; align-items:center;
}
.notice span{ font-weight:900; }

/* Cards y profundidad */
.cart-card{
  background:var(--cart-card);
  border:1px solid var(--cart-border);
  border-radius:22px;
  box-shadow:var(--cart-shadow), inset 0 0 0 1px rgba(255,255,255,.4);
  backdrop-filter:blur(12px) saturate(1.04);
}
.card-frame{
  border-radius:24px; padding:2px;
  background:
    conic-gradient(from var(--spin,0deg), rgba(96,165,250,.9), rgba(52,211,153,.9), rgba(251,191,36,.9), rgba(96,165,250,.9));
  animation: spin 10s linear infinite;
  box-shadow:0 10px 26px rgba(96,165,250,.22);
}
.card-frame > .cart-card{ border-radius:22px; }
@keyframes spin{ to{ --spin:360deg; } }

/* Item list */
.cart-item{
  display:grid; grid-template-columns:86px 1fr auto; gap:12px;
  align-items:center; padding:12px 14px;
  border:1px solid var(--cart-border); border-radius:18px;
  background:linear-gradient(180deg,#fff,rgba(255,255,255,.96));
  box-shadow:0 16px 40px rgba(15,23,42,.12), inset 0 0 0 1px rgba(255,255,255,.45);
  transform:translateY(0) scale(1);
  transition:transform .14s ease, box-shadow .14s ease, border-color .18s ease, background .18s ease, filter .2s;
}
.cart-item:hover{
  transform:translateY(-2px) scale(1.015);
  border-color:#a7f3d0;
  box-shadow:0 28px 84px rgba(15,23,42,.20), inset 0 0 0 1px rgba(255,255,255,.6);
  background:linear-gradient(180deg,#ffffff,rgba(240,253,244,.96));
  filter:saturate(1.06);
}
.item-body{ display:flex; flex-direction:column; gap:6px; }
.chips{ display:flex; gap:8px; flex-wrap:wrap; }
.chips span{
  font-size:.73rem; font-weight:800;
  border:1px solid #e2e8f0; padding:4px 8px; border-radius:999px;
  background:white;
}
.item-right{ display:flex; flex-direction:column; gap:8px; align-items:flex-end; }
.price-badge{
  background:linear-gradient(180deg,#ecfdf5,#fff);
  border:1px solid #a7f3d0; border-radius:12px; padding:6px 10px;
  box-shadow:0 10px 22px rgba(16,185,129,.20); color:#065f46 !important;
  font-weight:900;
}

/* Resumen */
.summary-brutal{ position:sticky; top:18px; }
.summary-head{ display:flex; align-items:center; justify-content:space-between; margin-bottom:6px; gap:8px; }
.chip-secure{
  border-radius:999px; padding:6px 10px; font-weight:800; font-size:.8rem;
  background:linear-gradient(90deg,#e0f2fe,#ecfeff);
  border:1px solid #bae6fd; box-shadow:0 10px 20px rgba(2,132,199,.12);
}
.summary-stats{
  display:grid; grid-template-columns:1fr 1fr 1fr; gap:10px; margin:8px 0 4px;
  background:linear-gradient(180deg,#ffffff,#f8fafc);
  border:1px solid #e5e7eb; border-radius:16px; padding:10px;
}
.stat{ background:transparent; box-shadow:none; border:none; }
.stat-l{ display:block; font-size:.75rem; color:#64748b; font-weight:700; }
.stat-v{ display:block; font-size:1.05rem; font-weight:900; color:#0f172a; }

.total-ink{
  font-weight:1000; font-size:28px; color:#0b6b53; letter-spacing:.2px;
  text-shadow:0 2px 0 rgba(16,185,129,.08);
}

/* CTA sobrio (sin ripple/tilt ni brillo radial) */
.btn-solid{ --ring:0 0 0 0 rgba(37,99,235,0); }
.cta-primary{
  position:relative; overflow:hidden;
  background:linear-gradient(135deg, #2563eb, #1d4ed8);
  color:white; font-weight:900; letter-spacing:.2px;
  border-radius:14px; box-shadow:0 18px 44px rgba(37,99,235,.26);
  transform:translateY(0);
  transition:transform .06s ease, box-shadow .18s ease, filter .14s ease;
}
.cta-primary:hover{
  transform:translateY(-1px);
  box-shadow:0 26px 66px rgba(37,99,235,.30);
  filter:saturate(1.02);
}
.cta-primary:active{ transform:none; box-shadow:0 16px 38px rgba(37,99,235,.22); }
.cta-primary::after{ display:none !important; } /* sin brillo */

/* Trustbar */
.trustbar{ display:flex; gap:10px; align-items:center; opacity:.95; flex-wrap:wrap; }
.trustbar > span{
  border:1px solid var(--cart-border); border-radius:999px; padding:7px 12px; font-weight:800;
  background:white; box-shadow:0 12px 26px rgba(15,23,42,.08);
  transition:filter .2s, transform .12s;
}
.trustbar > span:hover{ filter:grayscale(.2); transform:translateY(-1px); }

/* Ribbon */
.ribbon{
  margin-left:auto;
  background:linear-gradient(90deg,#fef3c7,#ffedd5);
  border:1px solid #fde68a; border-radius:999px; padding:4px 10px; font-weight:800; font-size:.8rem;
  box-shadow:0 10px 20px rgba(245,158,11,.16);
}

/* Scroller */
.cart-scroll{ max-height:58vh; }

/* Reveal on scroll */
.reveal{ opacity:0; transform:translateY(8px) scale(.995); will-change:transform,opacity; }
.reveal.visible{ opacity:1; transform:none; transition:opacity .5s ease, transform .5s ease; }

/* Tilt */
.tilt{ transform-style:preserve-3d; }

/* HR */
hr{
  border:none; height:1px;
  background:linear-gradient(90deg, transparent, #e5e7eb, transparent);
  margin:10px 0;
}

/* Dark mode */
[theme~="dark"] html, [theme~="dark"] body, [theme~="dark"] vaadin-app-layout{ background:#0b1220 !important; }
[theme~="dark"] #cart-root{
  --cart-card:rgba(17,24,39,.86); --cart-border:#1f2937; --cart-ink:#e5e7eb; --cart-ink-2:#cbd5e1;
}
[theme~="dark"] .card-frame{
  background:conic-gradient(from var(--spin,0deg), rgba(59,130,246,.55), rgba(16,185,129,.55), rgba(251,191,36,.45), rgba(59,130,246,.55));
  box-shadow:0 12px 32px rgba(59,130,246,.22);
}
[theme~="dark"] .notice{ background:linear-gradient(180deg,#0f172a,#0b1220); border-color:#1f2937; }
[theme~="dark"] .cart-card{ background:var(--cart-card); border-color:#1f2937; }
[theme~="dark"] .cart-item{ background:linear-gradient(180deg, rgba(17,24,39,.92), rgba(17,24,39,.86)); border-color:#1f2937; box-shadow:0 16px 40px rgba(0,0,0,.5); }
""";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('cart-decor-css')){const s=document.createElement('style');s.id='cart-decor-css';s.textContent=$0;document.head.appendChild(s);}else{document.getElementById('cart-decor-css').textContent=$0;}", css));
    }

    /** JS para parallax, reveal y tilt (SIN ripple en pagar) */
    private void injectCartJs() {
        UI.getCurrent().getPage().executeJs("""
          (function(){
            if(window.__cartFxLoaded) return; window.__cartFxLoaded=true;

            const root=document.getElementById('cart-root');
            if(!root) return;

            // Parallax leve con el ratón (para blobs/fondo)
            root.addEventListener('pointermove', e=>{
              const r=root.getBoundingClientRect();
              const x=(e.clientX - (r.left + r.width/2))/r.width;
              const y=(e.clientY - (r.top + r.height/2))/r.height;
              root.style.setProperty('--parx', (x*18).toFixed(2)+'px');
              root.style.setProperty('--pary', (y*10).toFixed(2)+'px');
            });

            // IntersectionObserver para reveal
            const io=new IntersectionObserver((entries)=>{
              entries.forEach(en=>{
                if(en.isIntersecting){ en.target.classList.add('visible'); io.unobserve(en.target); }
              });
            },{threshold:.12});
            root.querySelectorAll('.reveal').forEach(el=>io.observe(el));

            // Tilt 3D sutil (sólo elementos con .tilt; el botón pagar NO lo tiene)
            root.addEventListener('pointermove', (ev)=>{
              root.querySelectorAll('.tilt').forEach(el=>{
                const r=el.getBoundingClientRect();
                const cx=r.left + r.width/2, cy=r.top + r.height/2;
                const dx=(ev.clientX - cx)/r.width, dy=(ev.clientY - cy)/r.height;
                el.style.transform='rotateX('+(dy*-4)+'deg) rotateY('+(dx*6)+'deg) translateZ(0)';
              });
            });
            root.addEventListener('pointerleave', ()=>{
              root.querySelectorAll('.tilt').forEach(el=>{ el.style.transform='none'; });
            });
          })();
        """);
    }
}