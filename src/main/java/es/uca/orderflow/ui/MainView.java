package es.uca.orderflow.ui;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route("") // http://localhost:8080
public class MainView extends VerticalLayout {
  public MainView() {
    add(new H1("Front-Office listo con Vaadin + Spring + MySQL "));
    setSizeFull();
  }
}
