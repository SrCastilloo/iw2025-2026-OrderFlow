package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.*;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.notification.*;
import com.vaadin.flow.component.orderedlayout.*;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Caja;
import es.uca.orderflow.business.entities.Empleado;
import es.uca.orderflow.business.services.CajaService;
import es.uca.orderflow.persistence.data.CajaRepository;

import java.text.NumberFormat;
import java.util.Locale;

@PageTitle("Caja")
@Route("/backoffice/caja")
@AnonymousAllowed
public class CajaRecepcionistaView extends VerticalLayout {

    private final CajaService cajaService;
    private final CajaRepository cajaRepository;

    private final Empleado empleado;
    private final NumberFormat euro = NumberFormat.getCurrencyInstance(new Locale("es", "ES"));

    private final Span estadoLbl = new Span();
    private final Span openedLbl = new Span();
    private final Span totalsLbl = new Span();

    private final Button abrirBtn = new Button("Abrir caja");
    private final Button cerrarBtn = new Button("Cerrar caja");

    private final Grid<Caja> history = new Grid<>(Caja.class, false);

    public CajaRecepcionistaView(CajaService cajaService, CajaRepository cajaRepository) {
        this.cajaService = cajaService;
        this.cajaRepository = cajaRepository;

        this.empleado = (Empleado) VaadinSession.getCurrent().getAttribute("empleadoLogueado");
        if (empleado == null) {
            UI.getCurrent().navigate("/login");
            return;
        }

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        H2 title = new H2("Caja");
        title.getStyle().set("margin", "0");

        buildButtons();
        buildHistory();

        add(
                (Component) new HorizontalLayout(
                        new Button("Volver", e -> UI.getCurrent().navigate("/backoffice/recepcionista")),
                        title
                ),
                buildStatusCard(),
                new Hr(),
                new H3("Últimos cierres"),
                history
        );

        refresh();
    }

    private Component buildStatusCard() {
        VerticalLayout card = new VerticalLayout();
        card.getStyle()
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "12px")
                .set("padding", "16px")
                .set("max-width", "720px");

        estadoLbl.getStyle().set("font-weight", "800");
        openedLbl.getStyle().set("color", "var(--lumo-secondary-text-color)");
        totalsLbl.getStyle().set("font-weight", "700");

        HorizontalLayout actions = new HorizontalLayout(abrirBtn, cerrarBtn);
        actions.setSpacing(true);

        card.add(
                new H4("Estado"),
                estadoLbl,
                openedLbl,
                totalsLbl,
                actions
        );
        return card;
    }

    private void buildButtons() {
        abrirBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_PRIMARY);
        cerrarBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

        abrirBtn.addClickListener(e -> {
            try {
                cajaService.abrirCaja(empleado);
                Notification.show("Caja abierta.", 2000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                refresh();
            } catch (Exception ex) {
                Notification.show(ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        cerrarBtn.addClickListener(e -> {
            Dialog confirm = new Dialog();
            confirm.setHeaderTitle("Confirmar cierre de caja");
            confirm.add(new Paragraph("Se calculará la facturación desde la apertura de la caja hasta ahora (IVA 21%)."));

            Button ok = new Button("Cerrar caja", ev -> {
                try {
                    cajaService.cerrarCaja(empleado);
                    Notification.show("Caja cerrada y guardada.", 2500, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    confirm.close();
                    refresh();
                } catch (Exception ex) {
                    Notification.show(ex.getMessage(), 3000, Notification.Position.TOP_CENTER)
                            .addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            });
            ok.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_PRIMARY);

            Button cancel = new Button("Cancelar", ev -> confirm.close());
            cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            confirm.getFooter().add(cancel, ok);
            confirm.open();
        });
    }

    private void buildHistory() {
        history.addColumn(c -> c.getId()).setHeader("ID").setAutoWidth(true);
        history.addColumn(c -> c.isAbierta() ? "ABIERTA" : "CERRADA").setHeader("Estado").setAutoWidth(true);
        history.addColumn(Caja::getOpenedAt).setHeader("Apertura").setAutoWidth(true);
        history.addColumn(Caja::getClosedAt).setHeader("Cierre").setAutoWidth(true);
        history.addColumn(c -> euro.format(c.getTotalBase())).setHeader("Base").setAutoWidth(true);
        history.addColumn(c -> euro.format(c.getTotalIva())).setHeader("IVA").setAutoWidth(true);
        history.addColumn(c -> euro.format(c.getTotalConIva())).setHeader("Total").setAutoWidth(true);
        history.addColumn(Caja::getNumPedidos).setHeader("Pedidos").setAutoWidth(true);

        history.setHeight("360px");
    }

    private void refresh() {
        Caja abierta = cajaService.getCajaAbiertaOrNull();
        boolean isOpen = abierta != null;

        estadoLbl.setText(isOpen ? "ABIERTA" : "CERRADA");
        estadoLbl.getStyle().set("color", isOpen ? "var(--lumo-success-text-color)" : "var(--lumo-error-text-color)");

        if (isOpen) {
            openedLbl.setText("Apertura: " + abierta.getOpenedAt());
            totalsLbl.setText("Cierre pendiente. (Se calculará al cerrar).");
        } else {
            openedLbl.setText("No hay caja abierta ahora mismo.");
            totalsLbl.setText("Los clientes no pueden realizar pedidos mientras esté cerrada.");
        }

        abrirBtn.setEnabled(!isOpen);
        cerrarBtn.setEnabled(isOpen);

        history.setItems(cajaRepository.findTop10ByOrderByOpenedAtDesc());
    }
}
