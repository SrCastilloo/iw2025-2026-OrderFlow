// src/main/java/es/uca/orderflow/presentation/views/MetodosPagoView.java
package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.services.MetodoPagoConfigService;
import es.uca.orderflow.business.services.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@PageTitle("Métodos de pago")
@Route("/backoffice/pagos")
@AnonymousAllowed
public class MetodosPagoView extends Div {

    private final MetodoPagoConfigService service;
    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es","ES"));

    private final Div statusCard = new Div();
    private final UnorderedList list = new UnorderedList();
    private final Span badge = new Span();
    private final Paragraph priceP = new Paragraph();

    @Autowired
    public MetodosPagoView(MetodoPagoConfigService service) {
        this.service = service;
        setId("payconf-root");
        getStyle().set("maxWidth","980px").set("margin","20px auto").set("padding","0 16px");

        add(buildHeader(), buildCards());
        refresh();
        injectCss();
    }

    private Component buildHeader() {
        H2 h = new H2("Funcionalidad extra: Métodos de pago");
        h.getStyle().set("margin","0");
        Paragraph p = new Paragraph("Controla qué métodos de pago aparecen en el checkout del cliente.");
        Div d = new Div(h,p);
        d.getStyle().set("marginBottom","16px");
        return d;
    }

    private Component buildCards() {
        statusCard.getStyle()
                .set("border","1px solid var(--lumo-contrast-10pct)")
                .set("borderRadius","16px")
                .set("padding","16px")
                .set("background","var(--lumo-base-color)")
                .set("boxShadow","0 10px 24px rgba(15,23,42,.08)");

        H4 h = new H4("Estado del paquete de pagos");
        h.getStyle().set("marginTop","0");
        badge.getStyle()
                .set("display","inline-block").set("marginLeft","8px")
                .set("padding","4px 10px").set("borderRadius","999px")
                .set("fontWeight","800");

        Paragraph desc = new Paragraph(
                "Al desbloquear el paquete, tus clientes podrán pagar con Tarjeta, PayPal, Bizum y Transferencia. " +
                        "Ahora mismo, sólo está activo el método base (Tarjeta).");

        // Lista de lo que verá el cliente (Strong -> Span con negrita)
        Span strongTitle = new Span("Métodos visibles para el cliente:");
        strongTitle.getStyle().set("fontWeight","900");
        Div visible = new Div(strongTitle, list);
        visible.getStyle().set("marginTop","8px");
        list.getStyle().set("margin","8px 0");

        // Precio del pack + botón
        priceP.getStyle().set("fontWeight","800").set("color","#0f172a");

        Button unlock = new Button("Desbloquear ahora", VaadinIcon.CREDIT_CARD.create(), e -> onUnlock());
        unlock.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        unlock.getStyle()
                .set("borderRadius","12px")
                .set("background","linear-gradient(90deg,#2563eb,#1d4ed8)")
                .set("color","white")
                .set("boxShadow","0 12px 28px rgba(29,78,216,.28)");

        Div actions = new Div(priceP, unlock);
        actions.getStyle().set("display","flex").set("alignItems","center").set("gap","12px");

        statusCard.add(h, badge, desc, new Hr(), visible, new Hr(), actions);
        return statusCard;
    }

    private void refresh() {
        if (service == null) {
            // Salvaguarda opcional (no debería entrar aquí si Spring inyecta bien)
            badge.setText("SERVICIO NO DISPONIBLE");
            badge.getStyle().set("background","#fee2e2").set("color","#991b1b").set("border","1px solid #fecaca");
            list.removeAll();
            list.add(new ListItem("Tarjeta (modo básico)"));
            priceP.setText("Precio del paquete: —");
            return;
        }

        var cfg = service.getConfig();

        // Badge
        if (cfg.isFullUnlocked()) {
            badge.setText("DESBLOQUEADO");
            badge.getStyle().set("background","#dcfce7").set("color","#065f46").set("border","1px solid #86efac");
        } else {
            badge.setText("BLOQUEADO");
            badge.getStyle().set("background","#fee2e2").set("color","#991b1b").set("border","1px solid #fecaca");
        }

        // Métodos visibles
        list.removeAll();
        List<PaymentMethod> disp = service.getDisponibles();
        disp.forEach(m -> list.add(new ListItem(m.toString())));

        // Precio
        priceP.setText("Precio del paquete: " + euro.format(cfg.getPriceCents() / 100.0));
    }

    private void onUnlock() {
        var cfg = service.getConfig();
        if (cfg.isFullUnlocked()) {
            Notification.show("Ya está desbloqueado.").addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            return;
        }

        ConfirmDialog cd = new ConfirmDialog();
        cd.setHeader("Desbloquear métodos de pago");
        cd.setText("Se realizará un cargo único de " + euro.format(cfg.getPriceCents() / 100.0) +
                " a través de la pasarela externa. ¿Deseas continuar?");
        cd.setCancelable(true);
        cd.setConfirmText("Pagar y desbloquear");
        cd.setConfirmButtonTheme("primary");
        cd.addConfirmListener(e -> {
            try {
                // ownerEmail y token simulado. Puedes inyectar el email real si tienes sesión.
                var res = service.unlockAll("owner@tienda.local", "tok_test_visa");
                if (res.success()) {
                    Notification.show("¡Pago correcto! ID: " + res.txnId(), 3500, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    refresh();
                } else {
                    Notification.show(res.message(), 3500, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } catch (Exception ex) {
                Notification.show("Error al desbloquear: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        cd.open();
    }

    private void injectCss() {
        getUI().ifPresent(ui -> ui.getPage().executeJs(
                "if(!document.getElementById('payconf-css')){" +
                        " const s=document.createElement('style'); s.id='payconf-css';" +
                        " s.textContent=`" +
                        "  #payconf-root strong{font-weight:900}" +
                        "  #payconf-root ul{padding-left:18px}" +
                        "  #payconf-root li{margin:4px 0}" +
                        " `; document.head.appendChild(s);" +
                        "}"
        ));
    }
}
