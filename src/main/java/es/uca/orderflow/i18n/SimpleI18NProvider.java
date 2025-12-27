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

    private static final String BUNDLE_PREFIX = "i18n.messages";

    public static final Locale LOCALE_ES = new Locale("es");
    public static final Locale LOCALE_EN = new Locale("en");

    private static final List<Locale> supportedLocales = List.of(LOCALE_ES, LOCALE_EN);

    @Override
    public List<Locale> getProvidedLocales() {
        return supportedLocales;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        if (key == null) return "";

        // Normaliza el idioma
        String lang = (locale == null ? "es" : locale.getLanguage());

        ResourceBundle bundle;
        try {
            if ("en".equalsIgnoreCase(lang)) {
                // Esto cargará messages_en.properties
                bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, LOCALE_EN);
            } else {
                // Español = bundle base messages.properties
                bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, Locale.ROOT);
            }
        } catch (MissingResourceException e) {
            // Último fallback: base
            bundle = ResourceBundle.getBundle(BUNDLE_PREFIX, Locale.ROOT);
        }

        try {
            String value = bundle.getString(key);
            return (params != null && params.length > 0)
                    ? String.format(locale != null ? locale : LOCALE_ES, value, params)
                    : value;
        } catch (MissingResourceException e) {
            return "!" + key + "!";
        }
    }
}
