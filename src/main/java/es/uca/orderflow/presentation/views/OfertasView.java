package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Oferta;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.ProductoTipo;
import es.uca.orderflow.business.services.GestionarProducto;
import es.uca.orderflow.business.services.OfertaService;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@PageTitle("Ofertas")
@Route("/backoffice/ofertas")
@AnonymousAllowed
public class OfertasView extends VerticalLayout {

    private final OfertaService ofertaService;
    private final GestionarProducto gestionarProducto;

    private final Grid<Oferta> grid = new Grid<>(Oferta.class, false);

    private Oferta editing = null;

    private final TextField nombre = new TextField("Nombre");
    private final BigDecimalField descuentoPct = new BigDecimalField("Descuento (%)");
    private final IntegerField prioridad = new IntegerField("Prioridad");
    private final Checkbox activa = new Checkbox("Activa");

    private final DatePicker fechaInicio = new DatePicker("Fecha inicio");
    private final DatePicker fechaFin = new DatePicker("Fecha fin");
    private final TimePicker horaInicio = new TimePicker("Hora inicio");
    private final TimePicker horaFin = new TimePicker("Hora fin");
    private final TextField diasSemana = new TextField("Días semana (CSV: MON,TUE,...)");

    private final Checkbox aplicaATodos = new Checkbox("Aplica a todos los productos");
    private final ComboBox<ProductoTipo> aplicaATipo = new ComboBox<>("Aplica a tipo (opcional)");
    private final MultiSelectComboBox<Producto> productos = new MultiSelectComboBox<>("Productos (si no aplica a todos)");

    private final Button guardar = new Button("Guardar");
    private final Button cancelar = new Button("Cancelar edición");

    private static final BigDecimal CIEN = new BigDecimal("100");

    /**
     * IMPORTANTE: este campo es la clave para que el renderer de "Estado" se actualice
     * en cada reload(). Si lo capturas en un closure local, se queda con el valor viejo.
     */
    private Set<Long> vigentesIds = Collections.emptySet();

    public OfertasView(OfertaService ofertaService, GestionarProducto gestionarProducto) {
        this.ofertaService = ofertaService;
        this.gestionarProducto = gestionarProducto;

        setWidthFull();
        setMaxWidth("1200px");
        getStyle().set("margin", "0 auto");
        setPadding(true);
        setSpacing(true);

        add(new H2("Ofertas"));
        add(buildForm());
        add(buildGrid());

        reload();
    }

    private VerticalLayout buildForm() {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);

        nombre.setWidthFull();

        descuentoPct.setWidth("220px");
        descuentoPct.setHelperText("0–100 (porcentaje).");
        descuentoPct.setClearButtonVisible(true);

        // Clamp descuento a [0,100]
        descuentoPct.addValueChangeListener(e -> {
            BigDecimal v = e.getValue();
            if (v == null) return;
            if (v.compareTo(BigDecimal.ZERO) < 0) descuentoPct.setValue(BigDecimal.ZERO);
            else if (v.compareTo(CIEN) > 0) descuentoPct.setValue(CIEN);
        });

        prioridad.setWidth("180px");
        prioridad.setMin(0);
        prioridad.setStepButtonsVisible(true);
        prioridad.setValue(0);

        // Clamp prioridad >= 0
        prioridad.addValueChangeListener(e -> {
            Integer v = e.getValue();
            if (v != null && v < 0) prioridad.setValue(0);
        });

        activa.setValue(true);

        diasSemana.setWidthFull();
        diasSemana.setPlaceholder("Ej: MON,TUE,WED (vacío = cualquier día)");

        aplicaATipo.setItems(ProductoTipo.values());
        aplicaATipo.setClearButtonVisible(true);
        aplicaATipo.setWidth("260px");

        productos.setWidthFull();
        productos.setItemLabelGenerator(p -> p.getNombre() + " (id=" + p.getId() + ")");
        productos.setItems(gestionarProducto.consultarSoloProductos());

        aplicaATodos.addValueChangeListener(e -> {
            boolean all = Boolean.TRUE.equals(e.getValue());
            productos.setEnabled(!all);
            if (all) productos.clear();
        });

        guardar.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardar.addClickListener(e -> onSave());

        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelar.setEnabled(false);
        cancelar.addClickListener(e -> clearForm());

        HorizontalLayout row1 = new HorizontalLayout(nombre);
        row1.setWidthFull();

        HorizontalLayout row2 = new HorizontalLayout(descuentoPct, prioridad, activa);
        row2.setWidthFull();
        row2.setAlignItems(Alignment.END);

        HorizontalLayout row3 = new HorizontalLayout(fechaInicio, fechaFin, horaInicio, horaFin);
        row3.setWidthFull();
        row3.setAlignItems(Alignment.END);

        HorizontalLayout row4 = new HorizontalLayout(aplicaATodos, aplicaATipo);
        row4.setWidthFull();
        row4.setAlignItems(Alignment.CENTER);

        HorizontalLayout row5 = new HorizontalLayout(guardar, cancelar);
        row5.setWidthFull();

        wrap.add(row1, row2, row3, diasSemana, row4, productos, row5);
        wrap.getStyle()
                .set("padding", "12px")
                .set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("border-radius", "14px")
                .set("background", "var(--lumo-base-color)");

        return wrap;
    }

    private VerticalLayout buildGrid() {
        VerticalLayout wrap = new VerticalLayout();
        wrap.setPadding(false);
        wrap.setSpacing(true);

        grid.setWidthFull();

        grid.addColumn(Oferta::getId).setHeader("ID").setAutoWidth(true);
        grid.addColumn(Oferta::getNombre).setHeader("Nombre").setFlexGrow(1);

        grid.addColumn(o -> o.getDescuentoPct() == null
                        ? "—"
                        : o.getDescuentoPct().stripTrailingZeros().toPlainString() + "%")
                .setHeader("Descuento").setAutoWidth(true);

        grid.addColumn(Oferta::getPrioridad).setHeader("Prioridad").setAutoWidth(true);
        grid.addColumn(o -> o.isActiva() ? "Sí" : "No").setHeader("Activa").setAutoWidth(true);

        grid.addColumn(o -> o.getAplicaATipo() == null ? "—" : o.getAplicaATipo().name())
                .setHeader("Tipo").setAutoWidth(true);

        // IMPORTANTE: NO tocar o.getProductos() aquí (LAZY)
        grid.addColumn(o -> o.isAplicaATodos() ? "Todos" : "Seleccionados")
                .setHeader("Aplica a").setAutoWidth(true);

        // Estado: lee SIEMPRE el campo vigentesIds (actualizado en reload())
        grid.addColumn(new ComponentRenderer<>(o -> {
            boolean vigente = o.getId() != null && vigentesIds.contains(o.getId());
            Span s = new Span(vigente ? "Vigente ahora" : "No vigente");
            s.getStyle()
                    .set("padding", "3px 10px")
                    .set("border-radius", "999px")
                    .set("font-weight", "700");
            return s;
        })).setHeader("Estado").setAutoWidth(true);

        grid.addComponentColumn(o -> {
            Button editar = new Button("Editar", e -> loadToForm(o));
            editar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            Button toggle = new Button(o.isActiva() ? "Desactivar" : "Activar", e -> onToggle(o));
            toggle.addThemeVariants(ButtonVariant.LUMO_CONTRAST);

            Button del = new Button("Eliminar", e -> confirmDelete(o));
            del.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);

            return new HorizontalLayout(editar, toggle, del);
        }).setHeader("Acciones").setAutoWidth(true);

        wrap.add(grid);
        return wrap;
    }

    private void onSave() {
        try {
            String nom = valueOrNull(nombre);
            if (nom == null || nom.isBlank()) {
                notifyErr("El nombre es obligatorio.");
                return;
            }

            BigDecimal pct = descuentoPct.getValue();
            if (pct == null) {
                notifyErr("El descuento es obligatorio.");
                return;
            }
            // Clamp defensivo
            if (pct.compareTo(BigDecimal.ZERO) < 0) pct = BigDecimal.ZERO;
            if (pct.compareTo(CIEN) > 0) pct = CIEN;

            Integer prio = prioridad.getValue();
            if (prio != null && prio < 0) {
                notifyErr("La prioridad no puede ser negativa.");
                return;
            }

            if (fechaInicio.getValue() != null && fechaFin.getValue() != null &&
                    fechaFin.getValue().isBefore(fechaInicio.getValue())) {
                notifyErr("La fecha fin no puede ser anterior a la fecha inicio.");
                return;
            }

            if (horaInicio.getValue() != null && horaFin.getValue() != null &&
                    horaFin.getValue().isBefore(horaInicio.getValue())) {
                notifyErr("La hora fin no puede ser anterior a la hora inicio.");
                return;
            }

            Oferta o = new Oferta();
            o.setNombre(nom);
            o.setDescuentoPct(pct);
            o.setPrioridad(Optional.ofNullable(prio).orElse(0));
            o.setActiva(Boolean.TRUE.equals(activa.getValue()));

            o.setFechaInicio(fechaInicio.getValue());
            o.setFechaFin(fechaFin.getValue());
            o.setHoraInicio(horaInicio.getValue());
            o.setHoraFin(horaFin.getValue());
            o.setDiasSemana(valueOrEmpty(diasSemana));

            o.setAplicaATodos(Boolean.TRUE.equals(aplicaATodos.getValue()));
            o.setAplicaATipo(aplicaATipo.getValue());

            o.setProductos(new HashSet<>());
            if (!o.isAplicaATodos()) {
                o.getProductos().addAll(productos.getSelectedItems());
                if (o.getProductos().isEmpty()) {
                    notifyErr("Debes seleccionar al menos un producto o marcar 'aplica a todos'.");
                    return;
                }
            }

            if (editing == null) {
                ofertaService.crearOferta(o);
                notifyOk("Oferta creada.");
            } else {
                ofertaService.actualizarOferta(editing.getId(), o);
                notifyOk("Oferta actualizada.");
            }

            clearForm();
            reload();

        } catch (Exception ex) {
            notifyErr(ex.getMessage());
        }
    }

    private void onToggle(Oferta o) {
        try {
            ofertaService.toggleActiva(o.getId());
            reload();
        } catch (Exception ex) {
            notifyErr(ex.getMessage());
        }
    }

    private void confirmDelete(Oferta o) {
        ConfirmDialog dlg = new ConfirmDialog();
        dlg.setHeader("Eliminar oferta");
        dlg.setText("¿Eliminar la oferta \"" + o.getNombre() + "\"?");
        dlg.setCancelable(true);
        dlg.setConfirmText("Eliminar");
        dlg.setCancelText("Cancelar");
        dlg.setConfirmButtonTheme("error primary");
        dlg.addConfirmListener(e -> {
            try {
                ofertaService.eliminarOferta(o.getId());
                clearForm();
                reload();
                notifyOk("Oferta eliminada.");
            } catch (Exception ex) {
                notifyErr(ex.getMessage());
            }
        });
        dlg.open();
    }

    private void loadToForm(Oferta o) {
        editing = o;

        nombre.setValue(nvl(o.getNombre()));
        descuentoPct.setValue(o.getDescuentoPct());
        prioridad.setValue(o.getPrioridad());
        activa.setValue(o.isActiva());

        fechaInicio.setValue(o.getFechaInicio());
        fechaFin.setValue(o.getFechaFin());
        horaInicio.setValue(o.getHoraInicio());
        horaFin.setValue(o.getHoraFin());

        diasSemana.setValue(nvl(o.getDiasSemana()));

        aplicaATodos.setValue(o.isAplicaATodos());
        aplicaATipo.setValue(o.getAplicaATipo());

        boolean all = o.isAplicaATodos();
        productos.setEnabled(!all);
        productos.deselectAll();

        // Nota: esto toca LAZY. Con tu entidad ya corregida (Equals/HashCode solo id) suele ir bien.
        if (!all && o.getProductos() != null) {
            productos.select(o.getProductos());
        }

        cancelar.setEnabled(true);
        guardar.setText("Actualizar");
    }

    private void reload() {
        List<Oferta> items = ofertaService.listarTodas();

        // Actualiza el campo que usa el renderer
        this.vigentesIds = ofertaService.ofertasVigentesAhora().stream()
                .map(Oferta::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        grid.setItems(items);
        grid.getDataProvider().refreshAll();
    }

    private void clearForm() {
        editing = null;

        nombre.clear();
        descuentoPct.clear();
        prioridad.setValue(0);
        activa.setValue(true);

        fechaInicio.clear();
        fechaFin.clear();
        horaInicio.clear();
        horaFin.clear();

        diasSemana.clear();

        aplicaATodos.setValue(false);
        aplicaATipo.clear();

        productos.setEnabled(true);
        productos.deselectAll();

        cancelar.setEnabled(false);
        guardar.setText("Guardar");
    }

    private void notifyOk(String msg) {
        Notification.show(msg, 2500, Notification.Position.TOP_CENTER);
    }

    private void notifyErr(String msg) {
        Notification n = Notification.show("Error: " + (msg == null ? "Operación no válida." : msg),
                4000, Notification.Position.TOP_CENTER);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }

    private static String nvl(String s) {
        return s == null ? "" : s;
    }

    private static String valueOrNull(TextField tf) {
        String v = tf.getValue();
        return v == null ? null : v.trim();
    }

    private static String valueOrEmpty(TextField tf) {
        String v = tf.getValue();
        return v == null ? "" : v.trim();
    }
}
