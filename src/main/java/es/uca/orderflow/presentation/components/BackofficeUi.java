package es.uca.orderflow.presentation.components;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasStyle;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;

public final class BackofficeUi {

    private BackofficeUi() {}

    public static void applySoftBackground(HasStyle view) {
        view.getStyle().set("background",
                "radial-gradient(1000px 500px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                        "radial-gradient(900px 450px at 110% 8%, rgba(255,120,90,.28), transparent 60%)," +
                        "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)");
    }

    public static Component hero(VaadinIcon icon, String title, String subtitle) {
        Icon heroIcon = icon.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 h1 = new H1(title);
        h1.getStyle().set("margin", "0").set("letter-spacing", "-0.02em");

        Paragraph p = new Paragraph(subtitle);
        p.getStyle().set("margin", "6px 0 0 0").set("opacity", "0.85");

        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(h1, p));
        hero.setAlignItems(FlexComponent.Alignment.CENTER);
        hero.setSpacing(true);
        hero.setPadding(true);
        hero.getStyle().set("margin-top", "6vh").set("margin-bottom", "2vh");
        return hero;
    }

    public static void toastOk(String msg) {
        Notification n = Notification.show(msg, 2500, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    public static void toastError(String msg) {
        Notification n = Notification.show(msg, 3500, Notification.Position.MIDDLE);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
