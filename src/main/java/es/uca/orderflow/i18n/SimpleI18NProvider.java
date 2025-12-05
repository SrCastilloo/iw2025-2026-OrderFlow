package es.uca.orderflow.i18n;

import com.vaadin.flow.i18n.I18NProvider;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.MissingResourceException;
import org.springframework.stereotype.Component;

@Component
public class SimpleI18NProvider implements I18NProvider {

    // Define la ubicación del archivo base de propiedades
    private static final String BUNDLE_PREFIX = "i18n.messages";

    // Define los idiomas soportados
    public static final Locale LOCALE_ES = new Locale("es", "ES");
    public static final Locale LOCALE_EN = new Locale("en", "US");

    private static final List<Locale> supportedLocales =
            Collections.unmodifiableList(List.of(LOCALE_ES, LOCALE_EN));

    /**
     * Implementación de I18NProvider: Devuelve los idiomas disponibles.
     */
    @Override
    public List<Locale> getProvidedLocales() {
        return supportedLocales;
    }

    // 🚨 MÉTODO ELIMINADO: Ya no es parte de la interfaz I18NProvider en versiones recientes de Vaadin.
    /*
    @Override
    public List<Locale> getSupportedLocales() {
        return supportedLocales;
    }
    */

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) {
            return "";
        }

        ResourceBundle bundle;
        try {
            // Intenta cargar el bundle específico (ej. messages_es)
            bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, locale);
        } catch (MissingResourceException e) {
            // Si falla, usa el bundle base (messages)
            bundle = ResourceBundle.getBundle(BUNDLE_PREFIX);
        }

        try {
            String value = bundle.getString(key);

            // Formatea la cadena si se pasaron parámetros (ej. para inyectar nombres)
            if (params.length > 0) {
                // Usar String.format(locale, ...) es crucial para formateo numérico/fechas
                return String.format(locale, value, params);
            }
            return value;
        } catch (MissingResourceException e) {
            // Devuelve la clave rodeada de '!' si no se encuentra (para debug)
            return "!" + key + "!";
        }
    }
}