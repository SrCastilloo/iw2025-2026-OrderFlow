// src/main/java/es/uca/orderflow/ui/LoginView.java
package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.services.IdentificarCliente;
import es.uca.orderflow.presentation.views.MainLayout;
import es.uca.orderflow.presentation.views.RegistroView;

@Route(value = "login", layout = MainLayout.class)
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    private final IdentificarCliente identificar;

    public LoginView(IdentificarCliente identificar) {
        this.identificar = identificar;

        EmailField correo = new EmailField("Correo");
        PasswordField contrasena = new PasswordField("Contraseña");
        Button entrar = new Button("Entrar");

        entrar.addClickListener(e -> {
            try {
                Cliente c = identificar.identificaCliente(correo.getValue(), contrasena.getValue());
                // Guardar en sesión Vaadin (rápido)
                VaadinSession.getCurrent().setAttribute(Cliente.class, c);
                Notification.show("Bienvenido, " + c.getNombre());
                getUI().ifPresent(ui -> ui.navigate(RegistroView.class));
            } catch (Exception ex) {
                Notification.show("Error: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
            }
        });

        add(new Paragraph("Iniciar sesión"), correo, contrasena, entrar);
        setAlignItems(Alignment.START);
    }

    /** helper estático para cualquier vista */
    public static Cliente getCurrentCliente() {
        return (Cliente) VaadinSession.getCurrent().getAttribute(Cliente.class);
    }
}
