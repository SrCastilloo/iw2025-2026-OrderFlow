package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Duenno;
import es.uca.orderflow.business.entities.EstadoMesa;
import es.uca.orderflow.business.entities.Mesa;
import es.uca.orderflow.business.services.GestionarMesa;
import es.uca.orderflow.business.services.DuennoSesionService;
import org.springframework.beans.factory.annotation.Autowired;

@PageTitle("Crear Mesa")
@Route("/backoffice/crearmesa")
@AnonymousAllowed
public class CrearMesaView extends VerticalLayout implements BeforeEnterObserver {

    private final GestionarMesa gestionarMesa;
    private final DuennoSesionService duennoSesionService;
    private final TextField nombreField = new TextField("Nombre de la Mesa");
    private final Button guardarBtn = new Button("Guardar Mesa");
    private final Button volverBtn = new Button("Volver al Panel");
    private final Grid<Mesa> gridMesas = new Grid<>(Mesa.class);

    @Autowired
    public CrearMesaView(GestionarMesa gestionarMesa, DuennoSesionService duennoSesionService) {
        this.gestionarMesa = gestionarMesa;
        this.duennoSesionService = duennoSesionService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        // Formulario para crear una mesa
        FormLayout form = new FormLayout();
        form.add(nombreField);

        // Estilizamos el botón de guardar
        guardarBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        guardarBtn.addClickListener(e -> guardarMesa());
        guardarBtn.getStyle().set("width", "auto")
                .set("padding", "10px 20px")
                .set("background", "linear-gradient(90deg, #3f51b5, #2196f3)")
                .set("color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0px 8px 12px rgba(0, 0, 0, 0.1)")
                .set("transition", "0.3s ease-in-out");

        // Botón de volver con estilo mejorado
        volverBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        volverBtn.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/duennopanel")));
        volverBtn.getStyle().set("width", "auto")
                .set("padding", "10px 20px")
                .set("background", "linear-gradient(90deg, #f44336, #e57373)")
                .set("color", "white")
                .set("border-radius", "8px")
                .set("box-shadow", "0px 8px 12px rgba(0, 0, 0, 0.1)")
                .set("transition", "0.3s ease-in-out");

        // Configuración de la tabla de mesas
        configureGrid();

        // Layout principal con acciones
        HorizontalLayout actions = new HorizontalLayout(volverBtn);
        actions.setWidthFull();
        actions.setJustifyContentMode(JustifyContentMode.CENTER);

        add(actions, form, guardarBtn, gridMesas);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // Verificar si hay un dueño logueado
        Duenno actual = duennoSesionService.getActual();
        if (actual == null) {
            System.out.println("No hay dueño logueado, redirigiendo al login.");
            event.forwardTo(DuennoLoginView.class);
        } else {
            System.out.println("Dueño logueado: " + actual.getNombre());
        }
        loadMesas(); // Cargar las mesas al acceder a la vista
    }

    private void guardarMesa() {
        String nombre = nombreField.getValue();
        if (nombre == null || nombre.isEmpty()) {
            Notification.show("El nombre de la mesa es obligatorio", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
            return;
        }

        try {
            // Crear la nueva mesa
            Mesa nuevaMesa = new Mesa();
            nuevaMesa.setNombre(nombre);
            nuevaMesa.setEstado(EstadoMesa.LIBRE);  // El estado inicial es LIBRE

            // Llamar al servicio para guardar la mesa
            gestionarMesa.crearMesa(nuevaMesa);

            Notification.show("Mesa creada con éxito", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);

            // Limpiar el campo de nombre y recargar la lista
            nombreField.clear();
            loadMesas();

        } catch (Exception ex) {
            Notification.show("Error al crear la mesa: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }

    private void configureGrid() {
        // Configuración de la grilla para mostrar las mesas
        gridMesas.setColumns("nombre", "estado");
        gridMesas.addComponentColumn(mesa -> {
            Button editBtn = new Button("Editar", e -> editarMesa(mesa));
            Button deleteBtn = new Button("Eliminar", e -> eliminarMesa(mesa));

            Button toggleEstadoBtn = new Button(
                    mesa.getEstado() == EstadoMesa.LIBRE ? "Marcar ocupada" : "Marcar libre",
                    e -> {
                        EstadoMesa nuevo = mesa.getEstado() == EstadoMesa.LIBRE
                                ? EstadoMesa.OCUPADA
                                : EstadoMesa.LIBRE;
                        gestionarMesa.actualizarEstadoMesa(mesa.getId(), nuevo);
                        loadMesas();
                    }
            );

            HorizontalLayout buttonLayout = new HorizontalLayout(editBtn, deleteBtn, toggleEstadoBtn);
            buttonLayout.setAlignItems(Alignment.CENTER);
            return buttonLayout;
        }).setHeader("Acciones");


        gridMesas.setSizeFull();
        gridMesas.getStyle().set("margin-top", "20px")
                .set("border-radius", "10px")
                .set("border", "1px solid #ddd")
                .set("box-shadow", "0px 4px 12px rgba(0, 0, 0, 0.1)");
    }

    private void loadMesas() {
        // Cargar la lista de mesas ya creadas
        gridMesas.setItems(gestionarMesa.obtenerTodasLasMesas());
    }

    private void editarMesa(Mesa mesa) {
        // Lógica para editar la mesa
        Dialog dialog = new Dialog();
        TextField editNombreField = new TextField("Nombre de la mesa");
        editNombreField.setValue(mesa.getNombre());

        Button saveEditBtn = new Button("Guardar", e -> {
            mesa.setNombre(editNombreField.getValue());
            gestionarMesa.crearMesa(mesa);  // Usamos el método de crearMesa para actualizarla
            dialog.close();
            loadMesas();
            Notification.show("Mesa editada con éxito", 3000, Notification.Position.TOP_CENTER)
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        });
        dialog.add(editNombreField, saveEditBtn);
        dialog.open();
    }

    private void eliminarMesa(Mesa mesa) {
        // Confirmación y eliminación de la mesa
        Dialog confirmDialog = new Dialog();
        confirmDialog.add(new Text("¿Estás seguro de eliminar la mesa " + mesa.getNombre() + "?"));
        Button confirmButton = new Button("Eliminar", e -> {
            try {
                gestionarMesa.eliminarMesa(mesa);
                Notification.show("Mesa eliminada con éxito", 3000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                loadMesas();
            } catch (Exception ex) {
                Notification.show("Error al eliminar la mesa: " + ex.getMessage(), 5000, Notification.Position.TOP_CENTER)
                        .addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
            confirmDialog.close();
        });
        Button cancelButton = new Button("Cancelar", e -> confirmDialog.close());

        confirmDialog.add(confirmButton, cancelButton);
        confirmDialog.open();
    }
}
