package es.uca.orderflow.presentation.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;

import java.util.Collection;
import java.util.function.Consumer;

public final class VaadinRowUtils {

    private VaadinRowUtils() {}

    public static void forEachRowDivWithClass(Collection<Component> components,
                                              String cssClass,
                                              Consumer<Div> consumer) {
        if (components == null || components.isEmpty()) return;

        for (Component c : components) {
            if (c instanceof Div row && row.getElement().getClassList().contains(cssClass)) {
                consumer.accept(row);
            }
        }
    }
}
