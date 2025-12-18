package es.uca.orderflow.presentation.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.textfield.IntegerField;
import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.ProductoTipo;

import java.util.*;

public final class MenuComposicionMapper {
    private MenuComposicionMapper() {}

    public static Map<Long, Integer> buildMap(List<Component> children) {
        List<Div> rows = children.stream()
                .filter(c -> c instanceof Div && c.getElement().getClassList().contains("ing-row"))
                .map(c -> (Div) c)
                .toList();

        Map<Long, Integer> out = new LinkedHashMap<>();

        for (Div row : rows) {
            @SuppressWarnings("unchecked")
            ComboBox<Producto> cb = (ComboBox<Producto>) row.getComponentAt(0);
            IntegerField qty = (IntegerField) row.getComponentAt(1);

            Producto sel = cb.getValue();
            Integer cantidad = qty.getValue();

            if (sel == null && (cantidad == null || cantidad == 0)) continue;

            if (sel == null) throw new IllegalArgumentException("Hay una fila sin producto.");
            if (sel.getId() == null) throw new IllegalArgumentException("Producto inválido (sin id).");
            if (cantidad == null || cantidad < 1)
                throw new IllegalArgumentException("La cantidad de " + sel.getNombre() + " debe ser ≥ 1.");

            if (sel.getTipo() == ProductoTipo.MENU) {
                throw new IllegalArgumentException("Un menú no puede contener otro menú: " + sel.getNombre());
            }

            if (out.containsKey(sel.getId())) {
                throw new IllegalArgumentException("Producto repetido en el menú: " + sel.getNombre());
            }

            out.put(sel.getId(), cantidad);
        }

        return out;
    }
}
