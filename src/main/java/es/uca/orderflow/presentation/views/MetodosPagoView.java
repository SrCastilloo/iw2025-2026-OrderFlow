// src/main/java/es/uca/orderflow/presentation/views/MetodosPagoView.java
package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.services.MetodoPagoConfigService;
import es.uca.orderflow.business.services.PaymentMethod;
import org.springframework.beans.factory.annotation.Autowired;
import es.uca.orderflow.business.services.DuennoSesionService;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@PageTitle("Métodos de pago")
@Route("/backoffice/pagos")
@CssImport("./styles/payconf.clean.css")
@AnonymousAllowed
public class MetodosPagoView extends Div implements BeforeEnterObserver {

    private final MetodoPagoConfigService service;
    private final DuennoSesionService duennoSesionService;
    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es","ES"));

    private final Div card = new Div();
    private final UnorderedList list = new UnorderedList();
    private final Span badge = new Span();
    private final Paragraph priceP = new Paragraph();


    @Autowired
    public MetodosPagoView(MetodoPagoConfigService service, DuennoSesionService duennoSesionService) {
        this.service = service;
        this.duennoSesionService = duennoSesionService;
        setId("payconf-root");

        add(buildTopbar(), buildBody());
        refresh();
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (duennoSesionService.getActual() == null) {
            // si no hay dueño logueado -> mandar al login de dueño
            event.forwardTo(DuennoLoginView.class);
        }
    }

    private Component buildTopbar() {
        Div bar = new Div();
        bar.addClassName("pc-topbar");

        Button back = new Button("Volver", e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/duennopanel")));
        back.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        back.addClassName("pc-back");
        back.setPrefixComponent(VaadinIcon.ANGLE_LEFT.create());

        Div titles = new Div();
        titles.addClassName("pc-titles");
        H2 t = new H2("Funcionalidad extra: Métodos de pago");
        t.addClassName("pc-title");
        Paragraph s = new Paragraph("Controla qué métodos de pago aparecen en el checkout del cliente.");
        s.addClassName("pc-subtitle");
        titles.add(t, s);

        bar.add(back, titles);
        return bar;
    }

    private Component buildBody() {
        Div center = new Div();
        center.addClassName("pc-center");

        card.addClassName("pc-card");

        H4 h = new H4("Estado del paquete de pagos");
        h.addClassName("pc-card-title");

        badge.addClassName("pc-badge");

        Paragraph desc = new Paragraph(
                "Al desbloquear el paquete, tus clientes podrán pagar con Tarjeta, PayPal, Bizum y Transferencia. " +
                        "Ahora mismo, sólo está activo el método base (Tarjeta).");
        desc.addClassName("pc-desc");

        Span strong = new Span("Métodos visibles para el cliente:");
        strong.addClassName("pc-strong");
        list.addClassName("pc-list");

        Div visible = new Div(strong, list);
        visible.addClassName("pc-visible");

        priceP.addClassName("pc-price");

        Button unlock = new Button("Desbloquear ahora", e -> onUnlock());
        unlock.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        unlock.addClassName("pc-cta");
        unlock.setPrefixComponent(VaadinIcon.CREDIT_CARD.create());

        Div actions = new Div(priceP, unlock);
        actions.addClassName("pc-actions");

        card.add(h, badge, desc, new Hr(), visible, new Hr(), actions);
        center.add(card);
        return center;
    }

    private void refresh() {
        var cfg = service.getConfig();

        if (cfg.isFullUnlocked()) {
            badge.setText("DESBLOQUEADO");
            badge.removeClassName("ko");
            badge.addClassName("ok");
        } else {
            badge.setText("BLOQUEADO");
            badge.removeClassName("ok");
            badge.addClassName("ko");
        }

        list.removeAll();
        List<PaymentMethod> disp = service.getDisponibles();
        disp.forEach(m -> {
            ListItem li = new ListItem(m.toString());
            li.addClassName("pc-li");
            list.add(li);
        });

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
                ". ¿Deseas continuar?");
        cd.setCancelable(true);
        cd.setConfirmText("Pagar y desbloquear");
        cd.setConfirmButtonTheme("primary");
        cd.addConfirmListener(e -> {
            try {
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
}
