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

    public static List<Producto_Ingrediente> buildRelaciones(
            GestionarIngredientes gestionarIngredientes,
            Producto productoManaged,
            Collection<Component> rows
    ) {
        List<Producto_Ingrediente> out = new ArrayList<>();
        Set<Long> vistos = new HashSet<>();

        for (Component c : rows) {
            if (!(c instanceof Div row)) continue;
            if (!row.getElement().getClassList().contains("ing-row")) continue;

            @SuppressWarnings("unchecked")
            ComboBox<Ingrediente> cb = (ComboBox<Ingrediente>) row.getComponentAt(0);
            BigDecimalField qty = (BigDecimalField) row.getComponentAt(1);
            @SuppressWarnings("unchecked")
            ComboBox<String> unit = (ComboBox<String>) row.getComponentAt(2);

            Ingrediente sel = cb.getValue();
            BigDecimal cantidad = qty.getValue();
            String unidad = unit.getValue();

            boolean rowEmpty =
                    sel == null &&
                            (cantidad == null || BigDecimal.ZERO.compareTo(cantidad) == 0) &&
                            (unidad == null || unidad.isBlank());
            if (rowEmpty) continue;

            if (sel == null) throw new IllegalArgumentException("Hay una fila sin ingrediente.");
            if (cantidad == null || cantidad.compareTo(BigDecimal.ZERO) < 0)
                throw new IllegalArgumentException("La cantidad de " + sel.getNombre() + " debe ser ≥ 0.");
            if (unidad == null || unidad.isBlank())
                throw new IllegalArgumentException("La unidad de " + sel.getNombre() + " es obligatoria.");
            if (unidad.length() > 8)
                throw new IllegalArgumentException("La unidad para " + sel.getNombre() + " supera 8 caracteres.");
            if (!vistos.add(sel.getId()))
                throw new IllegalArgumentException("Ingrediente repetido: " + sel.getNombre());

            Ingrediente ingredienteManaged = gestionarIngredientes.obtenerIngredientePorId(sel.getId());

            Producto_Ingrediente pi = new Producto_Ingrediente();
            pi.setProducto(productoManaged);
            pi.setIngrediente(ingredienteManaged);
            pi.setCantidad(cantidad);
            pi.setUnidad(unidad);

            out.add(pi);
        }

        return out;
    }
}
