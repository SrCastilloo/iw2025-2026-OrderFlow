package es.uca.orderflow.presentation.components;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinSession;

import java.util.Locale;
import java.util.Map;


// Nota: Asumimos que I18NProvider se inyecta y es una instancia de SimpleI18NProvider

public class LanguageSelector extends HorizontalLayout {

    // Mapeo del Locale a la clave de traducción para mostrar el nombre del idioma
    // **Asegúrate de que estas claves (`lang.spanish`, `lang.english`) existan en tus .properties**
    private final Map<Locale, String> localeKeyMap = Map.of(
            new Locale("es", "ES"), "lang.spanish",
            new Locale("en", "US"), "lang.english"
    );

    public LanguageSelector(I18NProvider i18nProvider) {

        ComboBox<Locale> languageComboBox = new ComboBox<>();

        // Carga los locales definidos en tu SimpleI18NProvider (ej. es, en)
        languageComboBox.setItems(i18nProvider.getProvidedLocales());

        // Define cómo se muestra el texto en el ComboBox, usando su traducción:
        languageComboBox.setItemLabelGenerator(locale -> getTranslation(localeKeyMap.getOrDefault(locale, "lang.spanish")));

        // Establecer la selección inicial al Locale actual de la sesión (por defecto será el del navegador/sesión)
        languageComboBox.setValue(VaadinSession.getCurrent().getLocale());

        // Listener que detecta el cambio de selección
        languageComboBox.addValueChangeListener(event -> {
            Locale newLocale = event.getValue();
            if (newLocale != null && !newLocale.equals(VaadinSession.getCurrent().getLocale())) {
                changeLanguage(newLocale);
            }
        });

        // Configuración visual
        languageComboBox.setWidth("150px");
        add(languageComboBox);
    }

    /**
     * Lógica central para cambiar el idioma de la aplicación.
     */
    private void changeLanguage(Locale newLocale) {
        // 1. Establecer el nuevo Locale para la sesión actual
        VaadinSession.getCurrent().setLocale(newLocale);

        // 2. Establecer el nuevo Locale para la UI actual (buena práctica)
        UI.getCurrent().setLocale(newLocale);

        // 3. Forzar la recarga de la página
        // Esto hace que Vaadin reconstruya todos los componentes de la vista actual
        // usando el nuevo Locale guardado en la sesión, aplicando las traducciones.
        UI.getCurrent().getPage().reload();
    }
}