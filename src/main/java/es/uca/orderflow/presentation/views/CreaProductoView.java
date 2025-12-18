package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

import es.uca.orderflow.business.entities.Producto;
import es.uca.orderflow.business.entities.Producto_Ingrediente;
import es.uca.orderflow.business.services.GestionarIngredientes;
import es.uca.orderflow.business.services.GestionarProducto;
import es.uca.orderflow.presentation.components.BackofficeUi;
import es.uca.orderflow.presentation.components.ProductoForm;

import java.util.List;

@PageTitle("Crear Producto")
@Route("/backoffice/productos/crear")
@AnonymousAllowed
public class CreaProductoView extends VerticalLayout {

    private final GestionarProducto gestionarProducto;
    private final ProductoForm form;

    public CreaProductoView(GestionarProducto gestionarProducto,
                            GestionarIngredientes gestionarIngredientes) {
        this.gestionarProducto = gestionarProducto;

        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        BackofficeUi.applySoftBackground(this);

        var hero = BackofficeUi.hero(VaadinIcon.CUTLERY,
                "Crear un nuevo producto",
                "Completa los datos y confirma para publicarlo en el catálogo.");

        Div page = new Div();
        page.addClassName("page-wrap");
        add(page);

        Div card = new Div();
        card.addClassName("form-card");
        card.addClassName("form-card--loud");

        H3 blockTitle = new H3("Datos básicos");
        blockTitle.getStyle().set("margin", "0 0 10px 0");

        Hr sepTop = new Hr();
        sepTop.getStyle().set("margin", "10px 0 18px 0");

        this.form = new ProductoForm(gestionarIngredientes);

        Hr sepBottom = new Hr();
        sepBottom.getStyle().set("margin", "14px 0");

        HorizontalLayout actions = buildActions();

        card.add(blockTitle, sepTop, form, sepBottom, actions);
        page.add(hero, card);
    }

    private HorizontalLayout buildActions() {
        Button cancelar = new Button("Cancelar", VaadinIcon.ARROW_LEFT.create());
        cancelar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        cancelar.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate("/backoffice/duennopanel")));

        Button limpiar = new Button("Limpiar", VaadinIcon.ERASER.create());
        limpiar.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        limpiar.addClickListener(e -> form.reset());

        Button crear = new Button("Crear producto", VaadinIcon.PLUS_CIRCLE.create());
        crear.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        crear.addClickShortcut(Key.ENTER);
        crear.addClickListener(e -> onCrear());

        HorizontalLayout actions = new HorizontalLayout(cancelar, limpiar, crear);
        actions.addClassName("action-bar");
        actions.setWidthFull();
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END); // IMPORTANTE: no uses HorizontalLayout.JustifyContentMode
        return actions;
    }

    private void onCrear() {
        try {
            Producto producto = form.buildProducto();

            producto = gestionarProducto.crearProducto(producto);

            List<Producto_Ingrediente> relaciones = form.buildRelaciones(producto);
            if (relaciones.isEmpty()) {
                BackofficeUi.toastError("Añade al menos un ingrediente");
                return;
            }

            gestionarProducto.guardarProducto_Ingrediente(producto, relaciones);

            BackofficeUi.toastOk("Producto creado correctamente");
            form.reset();

        } catch (ValidationException ex) {
            BackofficeUi.toastError("Revisa los campos del formulario");
        } catch (IllegalArgumentException ex) {
            BackofficeUi.toastError(String.valueOf(ex.getMessage()));
        }
    }
}
