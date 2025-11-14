package es.uca.orderflow.presentation.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Tag;

@Tag("canvas")
public class HtmlCanvas extends Component implements HasSize {
    public HtmlCanvas() {
        setWidth("100%");
        setHeight("320px");
    }
}
