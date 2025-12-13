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
    private final CajaService cajaService;
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

        // 1) Intentar leer de query param ?modifying=...
        if (queryParams.getParameters().containsKey("modifying")) {
            try {
                Long id = Long.parseLong(queryParams.getParameters().get("modifying").get(0));
                modifyingPedidoId = id;
            } catch (NumberFormatException ignored) {
                // ID inválido, se ignora el modo modificación
            }
        }

        // 2) Si no vino por query param, mirar la sesión
        if (modifyingPedidoId == null) {
            Long idSesion = clienteSesionService.getPedidoEnModificacionId();
            if (idSesion != null) {
                modifyingPedidoId = idSesion;
            }
        }

        // 3) Título según estemos modificando o no
        if (modifyingPedidoId != null) {
            setPageTitle(getTranslation("view.cart.title_modifying", modifyingPedidoId));
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
                       I18NProvider i18nProvider,GestionarPedido gestionarPedido,
                       CajaService cajaService) {
        this.clienteSesionService = clienteSesionService;
        this.carritoQueryService = carritoQueryService;
        this.quitarProductoCarrito = quitarProductoCarrito;
        this.checkoutService = checkoutService;
        this.metodoPagoConfigService = metodoPagoConfigService;
        this.gestionarCarritoCliente = gestionarCarritoCliente;
        this.i18nProvider = i18nProvider;
        this.cajaService = cajaService;
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
        getStyle().set("background", "#ffffff");
        setPadding(false);
        setSpacing(false);
        setWidthFull();
        setHeightFull();

        // Fondo base BLANCO sin “hueco” abajo

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
        guardarBtn.setText(getTranslation("button.saving"));
        guardarBtn.setIcon(VaadinIcon.SPINNER.create());

        try {
            // ⭐ LLAMADA AL SERVICIO DE MODIFICACIÓN
            Long idGuardado = gestionarPedido.finalizarModificacionPedido(modifyingPedidoId, actual);

            // ⭐ Hemos terminado de modificar este pedido: limpiar estado en sesión
            clienteSesionService.limpiarPedidoEnModificacion();

            Dialog ok = new Dialog();
            ok.setHeaderTitle(getTranslation("dialog.modification_success.title"));
            ok.add(new Paragraph(getTranslation("dialog.modification_success.message", idGuardado)));

            Button goOrders = new Button(getTranslation("button.view_orders"), e -> {
                ok.close();
                UI.getCurrent().navigate("/cliente/pedidos");
            });
            ok.getFooter().add(goOrders);
            ok.open();

        } catch (Exception ex) {
            Notification.show(getTranslation("notification.modification_error", ex.getMessage()), 3500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        } finally {
            guardarBtn.setEnabled(true);
            guardarBtn.setText(getTranslation("button.confirm_modification"));
            guardarBtn.setIcon(VaadinIcon.PENCIL.create());
        }
    }




    private Component buildTopBar() {
        Div band = new Div();
        band.setWidthFull();
        band.addClassName("cart-topbar");
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
        boolean isModifying = modifyingPedidoId != null;
        boolean cajaAbierta = cajaService.isCajaAbierta();
        pagarBtn.setEnabled(cajaAbierta && !isModifying);

        if (!cajaAbierta && !isModifying) {
            Notification.show("Caja cerrada: no se pueden realizar pedidos ahora.",
                            2500, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        }


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

        pagarBtn.setVisible(!isModifying);
        guardarBtn.setVisible(isModifying);

        // Deshabilitar campos de pago si estamos modificando (pago ya realizado)
        direccionEnvio.setEnabled(!isModifying);
        metodoPago.setEnabled(!isModifying);
        confirm.setEnabled(!isModifying);

        // Si estamos modificando, el botón de guardar sólo se habilita si hay items
        if (isModifying) {
            boolean hasItems = !resumen.items().isEmpty();

            // Siempre permitimos guardar, incluso si no hay items (en ese caso se cancelará el pedido)
            guardarBtn.setEnabled(true);

            if (!hasItems) {
                // Texto indicando que guardar implicará cancelar
                guardarBtn.setText(getTranslation("button.save_will_cancel"));
            } else {
                guardarBtn.setText(getTranslation("button.confirm_modification"));
            }
            guardarBtn.setIcon(VaadinIcon.PENCIL.create());
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
        if (!cajaService.isCajaAbierta()) {
            Notification.show("La caja está cerrada. Vuelve a intentarlo más tarde.", 3000,
                    Notification.Position.TOP_CENTER).addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

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
    /** Reseteo global + soporte real de modo oscuro */
    /** Reseteo global: fondo blanco y sin “hueco” + soporte modo oscuro */
    /** Reseteo global: fondo blanco + modo oscuro con !important para ganar a otros CSS */
    private void injectGlobalResetCss() {
        String css = """
:root{
  /* LIGHT MODE BASE */
  --lumo-base-color:#ffffff;
  --lumo-primary-color:#ef4444;
  --lumo-body-text-color:#111827;
}

/* Fuerza fondo blanco en modo claro */
html, body,
vaadin-app-layout,
#outlet,
#outlet > *,
#cart-root,
.cart-shell{
  background:#ffffff !important;
  background-color:#ffffff !important;
  color:var(--lumo-body-text-color);
  min-height:100%;
}

/* ================= MODO OSCURO ================= */

html[theme~="dark"]{
  --lumo-base-color:#020617;
  --lumo-body-text-color:#e5e7eb;
  --lumo-primary-color:#f97316;
}

html[theme~="dark"],
html[theme~="dark"] body,
html[theme~="dark"] vaadin-app-layout,
html[theme~="dark"] #outlet,
html[theme~="dark"] #outlet > *,
html[theme~="dark"] #cart-root,
html[theme~="dark"] .cart-shell{
  background:#020617 !important;
  background-color:#020617 !important;
  color:var(--lumo-body-text-color);
}
""";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('cart-reset-css')){" +
                        "const s=document.createElement('style');s.id='cart-reset-css';s.textContent=$0;document.head.appendChild(s);" +
                        "}else{document.getElementById('cart-reset-css').textContent=$0;}", css));
    }




    /** CSS: brutal + alto contraste */
    /** CSS: brutal + alto contraste (light + dark bien definidos) */
    /** CSS: brutal + alto contraste (light + dark) */
    private void injectCartCss() {
        String css = """
/* ================= BASE LIGHT ================= */
#cart-root{
  --cart-accent:#ef4444;
  --cart-accent-2:#f59e0b;
  --cart-ink:#111827;
  --cart-ink-light:#ffffff;
  --cart-border:#e5e7eb;
  --cart-shadow:4px 4px 0 #111827;
  --cart-shadow-item:3px 3px 0 #111827;
  --cart-shadow-accent:4px 4px 0 var(--cart-accent);
  --cart-card:#ffffff;

  position:relative;
  isolation:isolate;
  background-color:#ffffff !important;
  background-image:none !important;
}

/* Blobs muy suaves */
#cart-root::before{
  content:"";
  position:absolute; inset:-8% -20% auto -20%;
  height:44vh; z-index:-1;
  background:
    radial-gradient(720px 360px at 12% 8%, rgba(239,68,68,.06), transparent 60%),
    radial-gradient(780px 380px at 88% 6%, rgba(245,158,11,.06), transparent 60%);
  filter:blur(20px);
}

#cart-root::after{
  content:""; position:absolute; inset:0; z-index:-1;
  pointer-events:none; opacity:.10;
  background-image:url("data:image/svg+xml;utf8,<svg xmlns='http://www.w3.org/2000/svg' width='120' height='120'><filter id='n'><feTurbulence type='fractalNoise' baseFrequency='0.85' numOctaves='2' stitchTiles='stitch'/><feColorMatrix type='saturate' values='0'/><feComponentTransfer><feFuncA type='table' tableValues='0 0 0.03 0'/></feComponentTransfer></filter><rect width='100%' height='100%' filter='url(%23n)'/></svg>");
  mix-blend-mode:multiply;
}

.cart-shell{ position:relative; z-index:1; }

/* Topbar */
.cart-topbar{
  background:linear-gradient(180deg, rgba(255,255,255,1), rgba(249,250,251,1)) !important;
  border-bottom:1px solid #e5e7eb;
  box-shadow:0 6px 18px rgba(15,23,42,.08);
}

#cart-root h2{
  color: var(--cart-ink) !important;
}
#cart-root h2::after{
  content:""; display:block; margin-top:6px; width:70px; height:6px;
  background: var(--cart-accent);
  box-shadow:2px 2px 0 #111827;
}

/* Pasos */
.cart-steps{ display:flex; gap:10px; align-items:center; margin-left:12px; }
.cart-steps span{
  font-weight:900; font-size:.9rem; color:var(--cart-ink);
  background:var(--cart-card);
  border:2px solid #111827;
  border-radius:0;
  padding:6px 12px;
  box-shadow:3px 3px 0 var(--cart-accent);
}
.cart-steps span:nth-child(1){ background: var(--cart-accent); color:#ffffff; }
.cart-steps span:nth-child(2){ background:#ffffff; color:#111827; box-shadow:3px 3px 0 #3b82f6; }
.cart-steps span:nth-child(3){ background:#111827; color:#f9fafb; box-shadow:3px 3px 0 var(--cart-accent); }

/* Cards */
.cart-card{
  background:#ffffff !important;
  border:2px solid var(--cart-border);
  border-radius:12px;
  box-shadow:0 18px 35px rgba(15,23,42,.12);
}

/* Marco resumen */
.card-frame{
  border-radius:16px;
  padding:4px;
  background:
    conic-gradient(from var(--spin,0deg), #ef4444 0%, #f59e0b 25%, #3b82f6 50%, #10b981 75%, #ef4444 100%);
  animation: spin 6s linear infinite;
  box-shadow:4px 4px 0 #111827, 0 10px 20px rgba(0,0,0,.25);
}
.card-frame > .cart-card{ border-radius:12px; }
@keyframes spin{ to{ --spin:360deg; } }

/* Items de producto */
.cart-item{
  display:grid; grid-template-columns:86px 1fr auto; gap:12px;
  align-items:center; padding:14px 16px;
  border:1px solid #e5e7eb;
  border-radius:14px;
  background:#ffffff !important;
  box-shadow:0 8px 18px rgba(15,23,42,.08);
  transform:none;
  transition:all .12s ease-in-out;
}
.cart-item img{
  border-radius:12px !important;
  border:1px solid #e5e7eb;
}

.cart-item:hover{
  transform: translateY(-2px);
  border-color:var(--cart-accent);
  box-shadow:0 14px 28px rgba(15,23,42,.22);
  background:#f9fafb !important;
}

/* Texto dentro del item (nombre, cantidad, precio) */
.cart-item span,
.cart-item p,
.cart-item label{
  color:#111827 !important;
}

.item-right{ display:flex; flex-direction:column; gap:8px; align-items:flex-end; }

.price-badge{
  background:var(--cart-accent);
  border-radius:999px;
  padding:4px 10px;
  color:#ffffff !important;
  font-weight:900;
}

/* Botón Eliminar muy visible */
#cart-root .btn-dangerish{
  background:#fee2e2 !important;
  color:#b91c1c !important;
  border-radius:999px;
  padding:0 10px;
  font-size:.8rem;
  border:none !important;
}

/* Stats */
.summary-stats{
  display:grid; grid-template-columns:1fr 1fr 1fr; gap:10px; margin:8px 0 4px;
  background:#f9fafb;
  border:1px solid #e5e7eb; border-radius:10px;
  padding:10px;
}
.stat-l{ display:block; font-size:.8rem; color:#4b5563; font-weight:600; }
.stat-v{ display:block; font-size:1.1rem; font-weight:900; color:var(--cart-accent); }

.total-ink{
  font-weight:900; font-size:32px;
  color:var(--cart-accent);
}

/* Botón pago */
.cta-primary{
  position:relative;
  background:var(--cart-accent);
  color:#ffffff !important;
  font-weight:900; letter-spacing:.5px;
  border:0;
  border-radius:999px;
  box-shadow:0 14px 30px rgba(239,68,68,.65);
  transition:all .12s ease-in-out;
}
.cta-primary:hover{
  transform:translateY(-3px);
  box-shadow:0 20px 40px rgba(239,68,68,.75);
  filter:brightness(1.05);
}

/* Ribbon */
.ribbon{
  background:var(--cart-accent-2);
  border-radius:999px;
  padding:4px 10px; font-weight:700; font-size:.85rem;
  color:#111827;
}

/* Scroll / reveal */
.cart-scroll{ max-height:60vh; }
.reveal{ opacity:0; transform:translateY(16px); }
.reveal.visible{ opacity:1; transform:none; transition:opacity .5s ease-out, transform .5s ease-out; }

/* ================= DARK MODE ================= */

html[theme~="dark"] #cart-root{
  --cart-accent:#f97316;
  --cart-accent-2:#eab308;
  --cart-ink:#e5e7eb;
  --cart-ink-light:#020617;
  --cart-border:#1f2937;
  --cart-card:#020617;
  background:#020617 !important;
}

/* topbar */
html[theme~="dark"] #cart-root .cart-topbar{
  background:linear-gradient(180deg, rgba(15,23,42,.96), rgba(15,23,42,.90)) !important;
  border-bottom:1px solid #1f2937;
  box-shadow:0 10px 30px rgba(0,0,0,.65);
}

/* cards & items */
html[theme~="dark"] #cart-root .cart-card{
  background:#020617 !important;
  border-color:#1f2937;
  box-shadow:0 18px 40px rgba(0,0,0,.85);
}
html[theme~="dark"] #cart-root .cart-item{
  background:#020617 !important;
  border-color:#1f2937;
  box-shadow:0 18px 30px rgba(0,0,0,.8);
}

/* textos */
html[theme~="dark"] #cart-root h2,
html[theme~="dark"] #cart-root h3,
html[theme~="dark"] #cart-root span,
html[theme~="dark"] #cart-root p,
html[theme~="dark"] #cart-root label{
  color:#e5e7eb !important;
}

/* precio */
html[theme~="dark"] #cart-root .price-badge{
  background:#f97316;
  color:#111827 !important;
}

/* botón eliminar en oscuro */
html[theme~="dark"] #cart-root .btn-dangerish{
  background:rgba(248,113,113,.18) !important;
  color:#fecaca !important;
}

/* CTA en oscuro */
html[theme~="dark"] #cart-root .cta-primary{
  background:#f97316;
  color:#111827 !important;
  box-shadow:0 18px 40px rgba(0,0,0,.9);
}
""";
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('cart-decor-css')){" +
                        "const s=document.createElement('style');s.id='cart-decor-css';s.textContent=$0;document.head.appendChild(s);" +
                        "}else{document.getElementById('cart-decor-css').textContent=$0;}", css));
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