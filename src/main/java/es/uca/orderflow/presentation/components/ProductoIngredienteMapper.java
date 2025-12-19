package es.uca.orderflow.presentation.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.BigDecimalField;
import es.uca.orderflow.business.entities.Ingrediente;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.business.services.GestionarIngredientes;

import java.math.BigDecimal;
import java.util.*;

public final class ProductoIngredienteMapper {

    private ProductoIngredienteMapper() {}

    private record RowData(Ingrediente ingrediente, BigDecimal cantidad, String unidad) {
        boolean isEmpty() {
            return ingrediente == null
                    && (cantidad == null || BigDecimal.ZERO.compareTo(cantidad) == 0)
                    && (unidad == null || unidad.isBlank());
        }
    }

    public static List<Producto_Ingrediente> buildRelaciones(
            GestionarIngredientes gestionarIngredientes,
            Producto productoManaged,
            Collection<Component> rows
    ) {
        Objects.requireNonNull(gestionarIngredientes, "gestionarIngredientes no puede ser null");
        Objects.requireNonNull(productoManaged, "productoManaged no puede ser null");

        List<Producto_Ingrediente> out = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();

        VaadinRowUtils.forEachRowDivWithClass(rows, "ing-row", row -> {
            RowData data = readRow(row);
            if (data.isEmpty()) return;

            validateRow(data, vistos);

            // Aseguramos entidad MANAGED (evita detached)
            Ingrediente ingredienteManaged = gestionarIngredientes.obtenerIngredientePorId(data.ingrediente().getId());

            out.add(toEntity(productoManaged, ingredienteManaged, data.cantidad(), data.unidad()));
        });

        return out;
    }

    private static RowData readRow(Div row) {
        @SuppressWarnings("unchecked")
        ComboBox<Ingrediente> cb = (ComboBox<Ingrediente>) row.getComponentAt(0);
        BigDecimalField qty = (BigDecimalField) row.getComponentAt(1);
        @SuppressWarnings("unchecked")
        ComboBox<String> unit = (ComboBox<String>) row.getComponentAt(2);

        Ingrediente sel = cb.getValue();
        BigDecimal cantidad = qty.getValue();
        String unidad = unit.getValue();

        return new RowData(sel, cantidad, unidad);
    }

    private static void validateRow(RowData data, Set<Long> vistos) {
        Ingrediente sel = data.ingrediente();
        BigDecimal cantidad = data.cantidad();
        String unidad = data.unidad();

        if (sel == null) throw new IllegalArgumentException("Hay una fila sin ingrediente.");
        if (sel.getId() == null) throw new IllegalArgumentException("Ingrediente inválido (sin id).");

        if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("La cantidad de " + sel.getNombre() + " debe ser ≥ 0.");
        }
        if (unidad == null || unidad.isBlank()) {
            throw new IllegalArgumentException("La unidad de " + sel.getNombre() + " es obligatoria.");
        }
        if (unidad.length() > 8) {
            throw new IllegalArgumentException("La unidad para " + sel.getNombre() + " supera 8 caracteres.");
        }
        if (!vistos.add(sel.getId())) {
            throw new IllegalArgumentException("Ingrediente repetido: " + sel.getNombre());
        }
    }

    private static Producto_Ingrediente toEntity(Producto productoManaged,
                                                 Ingrediente ingredienteManaged,
                                                 BigDecimal cantidad,
                                                 String unidad) {
        Producto_Ingrediente pi = new Producto_Ingrediente();
        pi.setProducto(productoManaged);
        pi.setIngrediente(ingredienteManaged);
        pi.setCantidad(cantidad);
        pi.setUnidad(unidad);
        return pi;
    }
}
