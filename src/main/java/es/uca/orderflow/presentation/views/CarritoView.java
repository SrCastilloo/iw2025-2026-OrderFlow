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
import com.vaadin.flow.i18n.I18NProvider;
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

@PageTitle("Mi carrito")
@Route("/cliente/carrito")
@AnonymousAllowed
public class CarritoView extends VerticalLayout implements BeforeEnterObserver {

    private final ClienteSesionService clienteSesionService;
    private final CarritoQueryService carritoQueryService;
    private final QuitarProductoCarrito quitarProductoCarrito;
    private final CheckoutService checkoutService;
    private final MetodoPagoConfigService metodoPagoConfigService;
    private final GestionarCarritoCliente gestionarCarritoCliente;
    private final GestionarPedido gestionarPedido;
    private final I18NProvider i18nProvider;
    private Long modifyingPedidoId = null;
    private final Button guardarBtn;
    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

    // UI
    private final Div list = new Div();
    private final Span totalLbl = new Span();
    private final TextField direccionEnvio;
    private final ComboBox<PaymentMethod> metodoPago;
    private final Checkbox confirm;
    private final Button pagarBtn;
    private  Button volverBtn =new Button(getTranslation("button.back"), VaadinIcon.ARROW_LEFT.create());

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        Cliente clienteActivo = clienteSesionService.getActual();

        if (clienteActivo == null) {
            event.forwardTo(LoginView.class);
            return;
        }

        // ⭐ Lógica de modificación
        modifyingPedidoId = null;
        var queryParams = event.getLocation().getQueryParameters();

        if (queryParams.getParameters().containsKey("modifying")) {
            try {
                Long id = Long.parseLong(queryParams.getParameters().get("modifying").get(0));
                modifyingPedidoId = id;

                // Si estamos modificando, el título de la vista cambia
                setPageTitle(getTranslation("view.cart.title_modifying", id));

            } catch (NumberFormatException ignored) {
                // ID inválido, se ignora el modo modificación
            }
        } else {
            setPageTitle(getTranslation("view.cart.title"));
        }


        reload(); // Refrescar la vista con el modo correcto
    }
    @Autowired
    public CarritoView(ClienteSesionService clienteSesionService,
                       CarritoQueryService carritoQueryService,
                       QuitarProductoCarrito quitarProductoCarrito,
                       CheckoutService checkoutService,
                       MetodoPagoConfigService metodoPagoConfigService,
                       GestionarCarritoCliente gestionarCarritoCliente,
                       I18NProvider i18nProvider,GestionarPedido gestionarPedido) {
        this.clienteSesionService = clienteSesionService;
        this.carritoQueryService = carritoQueryService;
        this.quitarProductoCarrito = quitarProductoCarrito;
        this.checkoutService = checkoutService;
        this.metodoPagoConfigService = metodoPagoConfigService;
        this.gestionarCarritoCliente = gestionarCarritoCliente;
        this.i18nProvider = i18nProvider;
        this.gestionarPedido = gestionarPedido;
        guardarBtn = new Button(getTranslation("button.confirm_modification"), VaadinIcon.PENCIL.create());
        guardarBtn.addClickListener(e -> onSaveModification());

        // Inicialización de componentes con textos traducidos
        direccionEnvio = new TextField(getTranslation("cart.shipping_address"));
        metodoPago = new ComboBox<>(getTranslation("cart.payment_method"));
        confirm = new Checkbox(getTranslation("cart.confirm_data"));
        pagarBtn = new Button(getTranslation("button.process_payment"), VaadinIcon.CREDIT_CARD.create());
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
        injectCartJs(); // micro-interacciones (parallax/reveal)

        add(buildTopBar(), buildContent());

        // Responsive grid
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "const css=`@media(max-width:980px){.cart-grid{grid-template-columns:1fr} }`;" +
                        "if(!document.getElementById('cart-css')){const s=document.createElement('style');s.id='cart-css';s.textContent=css;document.head.appendChild(s);}"));

        volverBtn.addClickListener(e -> {
            if (this.modifyingPedidoId != null) {
                UI.getCurrent().navigate("/cliente/pedidos");
            } else {
                UI.getCurrent().navigate("/cliente");
            }
        });
        reload();
    }

    private void onSaveModification() {
        if (modifyingPedidoId == null) return;

        var actual = clienteSesionService.getActual();
        if (actual == null) {
            Notification.show(getTranslation("notification.login_to_save")).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        // Deshabilitar y dar feedback visual
        guardarBtn.setEnabled(false);
        guardarBtn.setText(getTranslation("button.saving")); // TRADUCCIÓN: Guardando...
        guardarBtn.setIcon(VaadinIcon.SPINNER.create());

        try {
            // ⭐ LLAMADA AL SERVICIO DE MODIFICACIÓN
            Long idGuardado = gestionarPedido.finalizarModificacionPedido(modifyingPedidoId, actual);

            Dialog ok = new Dialog();
            ok.setHeaderTitle(getTranslation("dialog.modification_success.title")); // TRADUCCIÓN
            ok.add(new Paragraph(getTranslation("dialog.modification_success.message", idGuardado))); // TRADUCCIÓN

            Button goOrders = new Button(getTranslation("button.view_orders"), e -> { // TRADUCCIÓN
                ok.close();
                // Redirigir a la vista de pedidos para ver el cambio
                UI.getCurrent().navigate("/cliente/pedidos");
            });
            ok.getFooter().add(goOrders);
            ok.open();

        } catch (Exception ex) {
            // Muestra cualquier error de validación del servicio (ej. carrito vacío)
            Notification.show(getTranslation("notification.modification_error", ex.getMessage()), 3500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            // Revertir el estado del botón en caso de error
            guardarBtn.setEnabled(true);
            guardarBtn.setText(getTranslation("button.confirm_modification"));
            guardarBtn.setIcon(VaadinIcon.PENCIL.create());
            // No se llama a reload() ya que se asume que navegaremos fuera.
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
        bar.setAlignItems(Alignment.CENTER);
        bar.setPadding(true);

        // Indicador de pasos (visual)
        Div steps = new Div();
        steps.addClassName("cart-steps");
        // TRADUCCIÓN: Carrito de compra
        steps.add(new Span(getTranslation("cart.step_cart")), new Span(getTranslation("cart.step_of")), new Span(getTranslation("cart.step_purchase")));

        H2 title = new H2(getTranslation("view.cart.title")); // <-- TRADUCCIÓN
        title.getStyle().set("margin","0").set("font-weight","900");

        // ⭐ ESTILO BRUTAL DEL BOTÓN VOLVER
        volverBtn.removeThemeNames(ButtonVariant.LUMO_TERTIARY.getVariantName());
        volverBtn.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        volverBtn.addClassName("btn-subtle-back");


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
        right.addClassNames("cart-card", "cart-summary");
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
        // Botón de pago con estilo BRUTAL
        pagarBtn.addClassNames("cta-primary", "btn-solid");
        pagarBtn.setWidthFull();
        pagarBtn.addClickListener(e -> onPay());

        // Barra de confianza

        guardarBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        guardarBtn.addClassNames("cta-primary", "btn-solid");
        guardarBtn.setWidthFull();


        Div actionContainer = new Div();
        actionContainer.setWidthFull();
        actionContainer.add(pagarBtn, guardarBtn);



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
                actionContainer,
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
        boolean isModifying = modifyingPedidoId != null;

        pagarBtn.setVisible(!isModifying);
        guardarBtn.setVisible(isModifying);

        // Deshabilitar campos de pago si estamos modificando (pago ya realizado)
        direccionEnvio.setEnabled(!isModifying);
        metodoPago.setEnabled(!isModifying);
        confirm.setEnabled(!isModifying);

        // Si estamos modificando, el botón de guardar sólo se habilita si hay items
        if (isModifying) {
            boolean hasItems = !resumen.items().isEmpty();
            guardarBtn.setEnabled(hasItems);
            if (!hasItems) {
                guardarBtn.setText(getTranslation("button.add_items_to_save")); // TRADUCCIÓN: Añadir items para guardar
            } else {
                // Asegurarse de que el texto y el icono estén bien si antes estaba vacío
                guardarBtn.setText(getTranslation("button.confirm_modification"));
                guardarBtn.setIcon(VaadinIcon.PENCIL.create());
            }
        }

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
        // Se mantiene solo "cart-item"
        row.addClassName("cart-item");

        Image img = buildImage(item.foto(), item.nombre());
        img.setWidth(86, Unit.PIXELS);
        img.setHeight(66, Unit.PIXELS);
        img.getStyle().set("object-fit","cover").set("border-radius","12px"); // Mantener 12px para la imagen para que no sea *demasiado* feo.

        Div body = new Div();
        body.addClassName("item-body");
        Span name = new Span(item.nombre());
        name.getStyle().set("font-weight","900");
        Div chips = new Div();
        chips.addClassName("chips");

        // ⭐ CORRECCIÓN APLICADA AQUÍ: Se usan nuevas claves de traducción (sin el {0})
        // y se concatena el valor de forma explícita.

        // TRADUCCIÓN: Cantidad: X
        // Nota: Asume que "cart.item.quantity.label" en .properties es solo "Cantidad"
        Span qty = new Span(getTranslation("cart.item.quantity.label") + ": " + item.cantidad());
        qty.getStyle().set("color","var(--lumo-secondary-text-color)");

        // TRADUCCIÓN: Unitario: €X
        // Nota: Asume que "cart.item.unit_price.label" en .properties es solo "P. Unitario"
        Span unit = new Span(getTranslation("cart.item.unit_price.label") + ": " + euro.format(item.precioUnitario()));
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
  --lumo-base-color:#f9fafb; /* Fondo base claro, no blanco puro */
  --lumo-primary-color:#ef4444; /* Rojo Brutal */
  --lumo-body-text-color:#1f2937; /* Gris oscuro para el texto */
}
html, body{
  background:var(--lumo-base-color) !important;
  min-height:100%;
}
vaadin-app-layout, #outlet, #outlet > *{
  background:var(--lumo-base-color) !important;
}
#cart-root{
  min-height:100vh;
  background:var(--lumo-base-color);
  position:relative;
  overflow-x:hidden;
}
""";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('cart-reset-css')){const s=document.createElement('style');s.id='cart-reset-css';s.textContent=$0;document.head.appendChild(s);}else{document.getElementById('cart-reset-css').textContent=$0;}", css));
    }

    /** CSS: brutal + alto contraste */
    private void injectCartCss() {
        String css = """
#cart-root{
  --cart-accent:#ef4444; /* Rojo Impactante */
  --cart-accent-2:#f59e0b; /* Amarillo Fuerte */
  --cart-ink:#1f2937; /* Gris Oscuro (Tinta) */
  --cart-ink-light:#f9fafb; /* Fondo de Tarjeta muy claro */
  --cart-border:var(--cart-ink); /* Borde = Tinta */
  --cart-shadow:4px 4px 0 var(--cart-ink); /* Sombra dura (Brutal) */
  --cart-shadow-item:3px 3px 0 var(--cart-ink); /* Sombra dura para ítems */
  --cart-shadow-accent:4px 4px 0 var(--cart-accent); /* Sombra roja para CTA */
  --cart-card:var(--cart-ink-light); /* Fondo de tarjeta */

  position:relative;
  isolation:isolate;
  /* Fondo con patrón de rayas gruesas */
  background-color: var(--lumo-base-color);
  background-image:
    repeating-linear-gradient(45deg, var(--lumo-contrast-5pct) 0, var(--lumo-contrast-5pct) 2px, transparent 2px, transparent 10px);
}
/* Blobs (Se mantienen para contraste) */
#cart-root::before{
  content:"";
  position:absolute; inset:-8% -20% auto -20%;
  height:44vh; z-index:-1;
  background:
    radial-gradient(720px 360px at 12% 8%, rgba(239,68,68,.30), transparent 60%),
    radial-gradient(780px 380px at 88% 6%, rgba(245,158,11,.25), transparent 60%);
  filter:blur(20px);
  opacity:0.8;
}
/* Grain sutil */
#cart-root::after{
  content:""; position:absolute; inset:0; z-index:-1;
  pointer-events:none; opacity:.25;
  background-image:url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='120' height='120'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/><feColorMatrix type='saturate' values='0'/><feComponentTransfer><feFuncA type='table' tableValues='0 0 0.03 0'/></feComponentTransfer></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>");
  mix-blend-mode:multiply;
}
.cart-shell{ position:relative; z-index:1; }

/* Título */
h2{ color: var(--cart-ink); }
h2::after{
  content:""; display:block; margin-top:6px; width:70px; height:8px; /* Más grueso */
  background: var(--cart-accent);
  box-shadow:2px 2px 0 var(--cart-ink); /* Sombra dura */
}
.cart-steps{ display:flex; gap:10px; align-items:center; margin-left:12px; }
.cart-steps span{
  font-weight:900; font-size:.9rem; color:var(--cart-ink);
  background:var(--cart-card);
  border:2px solid var(--cart-ink); /* Borde grueso */
  border-radius:0; /* Cuadrado */
  padding:6px 12px;
  box-shadow:3px 3px 0 var(--cart-accent); /* Sombra de acento */
}
.cart-steps span:nth-child(1){ 
    background: var(--cart-accent); 
    color: var(--cart-ink-light); 
    border-color: var(--cart-ink);
} 
.cart-steps span:nth-child(2){ 
    background: var(--cart-ink-light); 
    color: var(--cart-ink); 
    border-color: var(--cart-ink);
    box-shadow: 3px 3px 0 #3b82f6; /* Sombra de contraste */
} 
.cart-steps span:nth-child(3){ 
    background: var(--cart-ink); 
    color: var(--cart-ink-light); 
    border-color: var(--cart-ink-light);
    box-shadow: 3px 3px 0 var(--cart-accent); 
} 

/* Botón Volver */
.orders-topbar vaadin-button.btn-subtle-back{
    font-family: monospace; /* Tipo de letra más 'brutal' */
    background: var(--cart-ink-light);
    color: var(--cart-ink);
    font-weight: 900;
    border: 2px solid var(--cart-ink);
    border-radius: 4px; 
    box-shadow: 3px 3px 0 var(--cart-accent);
    transition: all .1s ease-in-out;
}
.orders-topbar vaadin-button.btn-subtle-back:hover{
    transform: translate(-1px, -1px); 
    box-shadow: 4px 4px 0 var(--cart-accent);
}

/* Cards y profundidad */
.cart-card{
  background:var(--cart-card);
  border:3px solid var(--cart-border); /* Borde MUY GRUESO */
  border-radius:0; /* Cuadrado */
  box-shadow:var(--cart-shadow); /* Sombra dura */
  backdrop-filter:none; /* Quitar blur */
}
/* Marco de resumen (Animación Cónica Fuerte) */
.card-frame{
  border-radius:4px; 
  padding:4px; 
  background:
    conic-gradient(from var(--spin,0deg), #ef4444 0%, #f59e0b 25%, #3b82f6 50%, #10b981 75%, #ef4444 100%);
  animation: spin 6s linear infinite; 
  box-shadow:4px 4px 0 #1f2937, 0 10px 20px rgba(0,0,0,.3);
}
.card-frame > .cart-card{ border-radius:0; }
@keyframes spin{ to{ --spin:360deg; } }


/* Item list (Impactante) */
.cart-item{
  display:grid; grid-template-columns:86px 1fr auto; gap:12px;
  align-items:center; padding:14px 16px;
  border:2px solid var(--cart-border); 
  border-radius:0; 
  background:var(--cart-ink-light);
  box-shadow:var(--cart-shadow-item);
  transform:none;
  transition:all .1s ease-in-out; 
}
.cart-item img{ border:2px solid var(--cart-border); border-radius:0 !important; }

.cart-item:hover{
  transform: translate(-2px, -2px); /* Movimiento agresivo al hacer hover */
  border-color:var(--cart-accent);
  box-shadow:6px 6px 0 var(--cart-ink); /* Sombra dual */
  background: var(--lumo-contrast-5pct);
  filter:none;
}
.item-right{ display:flex; flex-direction:column; gap:8px; align-items:flex-end; }
.price-badge{
  background:var(--cart-accent); /* Rojo de acento */
  border:2px solid var(--cart-ink);
  border-radius:0; 
  padding:6px 10px;
  box-shadow:3px 3px 0 var(--cart-ink); 
  color:var(--cart-ink-light) !important;
  font-weight:900;
}
.item-body span:nth-child(1){ /* Nombre del producto */
  color: var(--cart-accent);
}

/* Resumen Stats */
.summary-stats{
  display:grid; grid-template-columns:1fr 1fr 1fr; gap:10px; margin:8px 0 4px;
  background:var(--cart-ink-light);
  border:2px solid var(--cart-border); border-radius:4px; 
  padding:10px;
}
.stat-l{ display:block; font-size:.8rem; color:var(--cart-ink); font-weight:900; }
.stat-v{ 
    display:block; font-size:1.1rem; font-weight:900; 
    color:var(--cart-accent); 
    text-shadow: 1px 1px 0 var(--cart-ink); /* Impresionante */
}

.total-ink{
  font-weight:900; font-size:32px; 
  color:var(--cart-accent); 
  text-shadow: 2px 2px 0 var(--cart-ink);
}

/* Botón de Pago (CTA) Brutal */
.cta-primary{
  position:relative; 
  background:var(--cart-accent);
  color:var(--cart-ink-light); font-weight:900; letter-spacing:.5px;
  border: 4px solid var(--cart-ink); /* Borde MUY GRUESO */
  border-radius: 0; 
  box-shadow:var(--cart-shadow-accent);
  transition:all .1s ease-in-out;
}
.cta-primary:hover{
  transform:translateY(-4px); /* Movimiento exagerado */
  box-shadow:8px 8px 0 var(--cart-ink), 0 10px 20px rgba(0,0,0,.4);
  filter:brightness(1.1);
}
.cta-primary:active{ transform:none; box-shadow:var(--cart-shadow-accent); }

/* HR: Más grueso */
hr{
  border:none; height:2px;
  background:var(--cart-border);
  margin:12px 0;
}

/* Ribbon */
.ribbon{
  background:var(--cart-accent-2);
  border:2px solid var(--cart-ink);
  border-radius:0; 
  padding:4px 10px; font-weight:900; font-size:.9rem;
  color: var(--cart-ink);
  box-shadow:2px 2px 0 var(--cart-ink);
}

/* Scroll y Reveal */
.cart-scroll{ max-height:60vh; }
.reveal{ opacity:0; transform:translateY(16px); will-change:transform,opacity; } 
.reveal.visible{ opacity:1; transform:none; transition:opacity .5s ease-out, transform .5s ease-out; }

/* Dark mode (Ajustado al nuevo esquema de color) */
[theme~="dark"] html, [theme~="dark"] body, [theme~="dark"] vaadin-app-layout{ background:#111827 !important; }
[theme~="dark"] #cart-root{
  --cart-accent:#fca5a5; /* Rojo más claro para fondo oscuro */
  --cart-ink:#f9fafb; /* Tinta clara */
  --cart-ink-light:#1f2937; /* Fondo de tarjeta oscuro */
  --cart-border:#f9fafb; /* Borde claro */
  --cart-card:var(--cart-ink-light); 
  --cart-shadow:4px 4px 0 var(--cart-border);
  --cart-shadow-item:3px 3px 0 var(--cart-border);
  --cart-shadow-accent:4px 4px 0 var(--cart-accent);
}
[theme~="dark"] .cart-steps span{ color: var(--cart-ink); border-color: var(--cart-border); }
[theme~="dark"] .cart-steps span:nth-child(1){ background: var(--cart-accent); color: var(--cart-ink-light); border-color: var(--cart-border); }
[theme~="dark"] .cart-steps span:nth-child(2){ background: var(--cart-ink-light); color: var(--cart-ink); border-color: var(--cart-border); }
[theme~="dark"] .cart-steps span:nth-child(3){ background: var(--cart-ink); color: var(--cart-ink-light); border-color: var(--cart-border); }

""";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('cart-decor-css')){const s=document.createElement('style');s.id='cart-decor-css';s.textContent=$0;document.head.appendChild(s);}else{document.getElementById('cart-decor-css').textContent=$0;}", css));
    }

    /** JS: Se elimina la animación de TILT 3D, manteniendo solo el Parallax y Reveal. */
    private void injectCartJs() {
        UI.getCurrent().getPage().executeJs("""
          (function(){
            if(window.__cartFxLoaded) return; window.__cartFxLoaded=true;

            const root=document.getElementById('cart-root');
            if(!root) return;

            // Parallax leve con el ratón (se mantiene para el impacto de fondo)
            root.addEventListener('pointermove', e=>{
              const r=root.getBoundingClientRect();
              const x=(e.clientX - (r.left + r.width/2))/r.width;
              const y=(e.clientY - (r.top + r.height/2))/r.height;
              // Ajuste el valor del parallax para hacerlo más sutil
              root.style.setProperty('--parx', (x*12).toFixed(2)+'px');
              root.style.setProperty('--pary', (y*6).toFixed(2)+'px');
            });

            // IntersectionObserver para reveal (se mantiene para la entrada de elementos)
            const io=new IntersectionObserver((entries)=>{
              entries.forEach(en=>{
                if(en.isIntersecting){ en.target.classList.add('visible'); io.unobserve(en.target); }
              });
            },{threshold:.08}); // Umbral más bajo
            root.querySelectorAll('.reveal').forEach(el=>io.observe(el));

            // ⭐ NOTA: Se ha eliminado toda la lógica del TILT 3D.

          })();
        """);
    }
}