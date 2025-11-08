package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.entities.Detalle_Carrito;
import es.uca.orderflow.business.entities.Pedido;
import es.uca.orderflow.business.services.GestionarCarrito;
import es.uca.orderflow.business.services.GestionarPedido;
import es.uca.orderflow.business.services.GestionarProducto;
import es.uca.orderflow.persistence.data.ClienteRepository;
import com.vaadin.flow.component.grid.Grid;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Route("/PedidosUsuario")
@Transactional
@AnonymousAllowed
public class PedidosView extends VerticalLayout {

    private GestionarPedido gestionarPedido;
    private ClienteRepository clienteRepository;

    public PedidosView(GestionarPedido gestionarPedido,  ClienteRepository clienteRepository) {
        this.clienteRepository = clienteRepository;
        this.gestionarPedido = gestionarPedido;
        Long clienteId = (Long) VaadinSession.getCurrent().getAttribute("clienteId");
        if (clienteId == null) {
            Notification.show("Debes iniciar sesión primero");
            getUI().ifPresent(ui -> ui.navigate("login"));
            return;
        }

        setSizeFull();
        setPadding(true);
        Set<Pedido> pedidos_usuario = gestionarPedido.getPedidosCliente(clienteId);

        Grid<Pedido> grid = new Grid<>();

        grid.addColumn(dc -> dc.getCliente().getNombre()).setHeader("Nombre Cliente");

        grid.setItems(pedidos_usuario);
        add(grid);
    }
}
