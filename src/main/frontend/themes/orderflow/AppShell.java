package es.uca.orderflow;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.theme.Theme;

@Theme("orderflow")  //
@PWA(name = "OrderFlow", shortName = "OrderFlow")
public class AppShell implements AppShellConfigurator {}
