package es.uca.orderflow.presentation.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import es.uca.orderflow.business.entities.Cliente;
import es.uca.orderflow.business.services.ClienteSesionService;
import es.uca.orderflow.i18n.SimpleI18NProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Route("/login")
@AnonymousAllowed
public class LoginView extends VerticalLayout {

    private static final String LOCALE_SESSION_ATTRIBUTE_KEY = "userLocale";

    private static final String LIGHT_BG =
            "radial-gradient(1200px 600px at 20% -10%, rgba(255,200,150,.35), transparent 60%)," +
                    "radial-gradient(1000px 500px at 110% 10%, rgba(255,120,90,.35), transparent 60%)," +
                    "linear-gradient(180deg, #fff5ef 0%, #ffe9d9 100%)";

    private static final String BTN_PRIMARY_BG =
            "linear-gradient(135deg, hsl(14,90%,55%), hsl(10,90%,50%))";

    private static final String BTN_PRIMARY_SHADOW_NORMAL =
            "0 14px 40px rgba(255, 94, 58, .35)";

    private static final String BTN_PRIMARY_SHADOW_HOVER =
            "0 18px 50px rgba(255, 94, 58, .45)";

    private static final List<LocaleOption> SUPPORTED_LOCALES = List.of(
            new LocaleOption("🇪🇸 Español", new Locale("es")),
            new LocaleOption("🇬🇧 English", new Locale("en"))
    );

    private final ClienteSesionService clienteSesionService;
    private final SimpleI18NProvider i18nProvider;

    @Autowired
    public LoginView(ClienteSesionService clienteSesionService, SimpleI18NProvider i18nProvider) {
        this.clienteSesionService = clienteSesionService;
        this.i18nProvider = i18nProvider;

        setupRootLayout();
        injectSoftGlowCss();

        Locale initialLocale = resolveInitialLocale();
        UI.getCurrent().setLocale(initialLocale);

        HorizontalLayout topRight = buildLanguageBar(initialLocale);
        HorizontalLayout hero = buildHero();

        EmailField email = buildEmailField();
        PasswordField password = buildPasswordField();

        FormLayout form = buildForm(email, password);

        Button acceder = buildPrimaryCtaButton();
        Button goRegister = buildRegisterLinkButton();

        Div card = buildCard(form, acceder, goRegister);

        add(topRight, hero, card);

        bindLogin(acceder, email, password);
    }

    /* ========================= SETUP / UI FACTORIES ========================= */

    private void setupRootLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.CENTER);

        getStyle().set("background", LIGHT_BG);

        // Paleta
        getElement().getStyle().set("--lumo-primary-color", "hsl(14, 90%, 55%)");
        getElement().getStyle().set("--lumo-primary-text-color", "hsl(14, 90%, 32%)");
        getElement().getStyle().set("--lumo-success-color", "hsl(135, 60%, 38%)");
        getElement().getStyle().set("--lumo-error-color", "hsl(0, 85%, 55%)");
        getElement().getStyle().set("--lumo-border-radius-l", "1.2rem");
        getElement().getStyle().set("--lumo-border-radius-m", "1rem");
    }

    private void injectSoftGlowCss() {
        Element style = new Element("style");
        style.setText("""
            @keyframes softGlow {
              0%   { box-shadow: 0 24px 60px rgba(255, 92, 53, .18); }
              50%  { box-shadow: 0 28px 70px rgba(255, 92, 53, .28); }
              100% { box-shadow: 0 24px 60px rgba(255, 92, 53, .18); }
            }
        """);
        getElement().appendChild(style);
    }

    private Locale resolveInitialLocale() {
        VaadinSession session = VaadinSession.getCurrent();
        Locale persisted = (Locale) session.getAttribute(LOCALE_SESSION_ATTRIBUTE_KEY);

        Locale current = persisted != null ? persisted : UI.getCurrent().getLocale();
        return SUPPORTED_LOCALES.stream()
                .map(LocaleOption::locale)
                .filter(l -> l.getLanguage().equals(current.getLanguage()))
                .findFirst()
                .orElse(new Locale("es"));
    }

    private HorizontalLayout buildLanguageBar(Locale initialLocale) {
        Select<Locale> languageSelect = new Select<>();
        languageSelect.setLabel("");
        languageSelect.setItems(SUPPORTED_LOCALES.stream().map(LocaleOption::locale).toList());
        languageSelect.setItemLabelGenerator(this::labelForLocale);
        languageSelect.setValue(initialLocale);

        languageSelect.getStyle()
                .set("width", "160px")
                .set("margin-top", "20px");

        languageSelect.addValueChangeListener(event -> {
            Locale newLocale = event.getValue();
            if (newLocale == null) return;
            if (newLocale.equals(UI.getCurrent().getLocale())) return;

            VaadinSession.getCurrent().setAttribute(LOCALE_SESSION_ATTRIBUTE_KEY, newLocale);
            UI.getCurrent().setLocale(newLocale);
            UI.getCurrent().getPage().reload();
        });

        HorizontalLayout topRight = new HorizontalLayout(languageSelect);
        topRight.setWidthFull();
        topRight.setJustifyContentMode(JustifyContentMode.END);
        topRight.getStyle().set("padding-right", "30px");
        return topRight;
    }

    private String labelForLocale(Locale locale) {
        return SUPPORTED_LOCALES.stream()
                .filter(o -> o.locale().getLanguage().equals(locale.getLanguage()))
                .map(LocaleOption::label)
                .findFirst()
                .orElse(locale.getDisplayLanguage(locale));
    }

    private HorizontalLayout buildHero() {
        Icon heroIcon = VaadinIcon.CUTLERY.create();
        heroIcon.getStyle()
                .set("font-size", "42px")
                .set("padding", "14px")
                .set("border-radius", "20px")
                .set("background", "linear-gradient(135deg, rgba(255,141,67,.25), rgba(255,77,77,.25))")
                .set("box-shadow", "0 10px 30px rgba(255,99,71,.28)")
                .set("backdrop-filter", "blur(6px)");

        H1 title = new H1(tr("view.login.title"));
        title.getStyle()
                .set("margin", "0")
                .set("font-size", "clamp(28px, 3vw, 40px)")
                .set("letter-spacing", "-0.02em")
                .set("color", "hsl(14, 90%, 24%)");

        Paragraph subtitle = new Paragraph(tr("hero.subtitle.login"));
        subtitle.getStyle()
                .set("margin", "6px 0 0 0")
                .set("font-size", "clamp(14px, 2vw, 16px)")
                .set("opacity", "0.85");

        HorizontalLayout hero = new HorizontalLayout(heroIcon, new Div(title, subtitle));
        hero.setAlignItems(FlexComponent.Alignment.CENTER);
        hero.setSpacing(true);
        hero.setPadding(true);
        hero.getStyle().set("margin-top", "2vh").set("margin-bottom", "2vh");
        return hero;
    }

    private Div buildCard(Component... content) {
        Div card = new Div();
        card.getStyle()
                .set("width", "min(640px, 94vw)")
                .set("padding", "28px")
                .set("border-radius", "26px")
                .set("background", "rgba(255,255,255,.78)")
                .set("backdrop-filter", "blur(12px)")
                .set("border", "1px solid rgba(255, 120, 90, .25)")
                .set("animation", "softGlow 6s ease-in-out infinite");

        VerticalLayout inner = new VerticalLayout(content);
        inner.setSpacing(false);
        inner.setPadding(false);
        inner.setWidthFull();

        card.add(inner);
        return card;
    }

    private EmailField buildEmailField() {
        EmailField email = new EmailField(tr("field.username"));
        email.setPlaceholder(tr("field.email_placeholder"));
        email.setClearButtonVisible(true);
        email.setRequired(true);
        email.setPrefixComponent(new Icon(VaadinIcon.ENVELOPE));

        email.addValueChangeListener(e -> {
            String v = Optional.ofNullable(e.getValue()).orElse("").trim();
            boolean ok = v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
            email.setInvalid(!ok);
            if (!ok) email.setErrorMessage(tr("validation.email_invalid"));
        });

        return email;
    }

    private PasswordField buildPasswordField() {
        PasswordField password = new PasswordField(tr("field.password"));
        password.setPlaceholder(tr("field.password_placeholder"));
        password.setRequired(true);
        password.setPrefixComponent(new Icon(VaadinIcon.LOCK));

        password.addValueChangeListener(e -> {
            boolean ok = e.getValue() != null && !e.getValue().isBlank();
            password.setInvalid(!ok);
            if (!ok) password.setErrorMessage(tr("validation.required"));
        });

        return password;
    }

    private FormLayout buildForm(EmailField email, PasswordField password) {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("520px", 2)
        );

        form.add(email, password);
        form.setColspan(email, 2);
        form.setColspan(password, 2);
        return form;
    }

    private Button buildPrimaryCtaButton() {
        Button acceder = new Button(tr("button.login"));
        acceder.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE, ButtonVariant.LUMO_SUCCESS);
        acceder.setIcon(VaadinIcon.SIGN_IN.create());
        acceder.setIconAfterText(true);
        acceder.addClickShortcut(Key.ENTER);

        acceder.getStyle()
                .set("width", "100%")
                .set("margin-top", "10px")
                .set("padding", "16px 20px")
                .set("font-weight", "800")
                .set("letter-spacing", ".3px")
                .set("border-radius", "18px")
                .set("background", BTN_PRIMARY_BG)
                .set("box-shadow", BTN_PRIMARY_SHADOW_NORMAL)
                .set("transform-origin", "center");

        addHoverLift(acceder, BTN_PRIMARY_SHADOW_HOVER, BTN_PRIMARY_SHADOW_NORMAL);
        return acceder;
    }

    private Button buildRegisterLinkButton() {
        Button goRegister = new Button(tr("link.go_register"));
        goRegister.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        goRegister.getStyle()
                .set("margin-top", "4px")
                .set("color", "hsl(14, 90%, 35%)")
                .set("font-size", "14px")
                .set("font-weight", "600")
                .set("text-decoration", "underline")
                .set("cursor", "pointer");

        goRegister.addClickListener(e -> getUI().ifPresent(ui -> ui.navigate(RegistroView.class)));
        return goRegister;
    }

    private void addHoverLift(Button b, String shadowHover, String shadowNormal) {
        b.getElement().getStyle().set("transition", "transform .08s ease, box-shadow .2s ease");
        b.getElement().addEventListener("mouseenter", e -> {
            b.getStyle().set("transform", "translateY(-1px)");
            b.getStyle().set("box-shadow", shadowHover);
        });
        b.getElement().addEventListener("mouseleave", e -> {
            b.getStyle().set("transform", "translateY(0)");
            b.getStyle().set("box-shadow", shadowNormal);
        });
    }

    /* ========================= LOGIN FLOW ========================= */

    private void bindLogin(Button acceder, EmailField email, PasswordField password) {
        acceder.addClickListener(e -> doLogin(acceder, email, password));
    }

    private void doLogin(Button acceder, EmailField email, PasswordField password) {
        if (!isFormValid(email, password)) {
            notifyError(tr("notification.review_credentials"), Notification.Position.MIDDLE);
            return;
        }

        setButtonLoading(acceder, true);

        try {
            String correo = Optional.ofNullable(email.getValue()).orElse("").trim();
            String pass = Optional.ofNullable(password.getValue()).orElse("");

            Cliente cliente = clienteSesionService.login(correo, pass);

            if (cliente == null) {
                notifyError(tr("error.invalid_credentials"), Notification.Position.MIDDLE);
                return;
            }

            authenticate(cliente);
            notifySuccess(tr("notification.welcome", cliente.getNombre()), Notification.Position.MIDDLE);
            navigateToClienteHome();

        } catch (Exception ex) {
            notifyError(tr("error.login_failed", ex.getMessage()), Notification.Position.MIDDLE);
        } finally {
            setButtonLoading(acceder, false);
        }
    }

    private boolean isFormValid(EmailField email, PasswordField password) {
        boolean emailOk = email.getValue() != null && !email.getValue().isBlank() && !email.isInvalid();
        boolean passOk = password.getValue() != null && !password.getValue().isBlank() && !password.isInvalid();
        return emailOk && passOk;
    }

    private void authenticate(Cliente cliente) {
        Authentication auth = new UsernamePasswordAuthenticationToken(cliente, null, new ArrayList<>());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void navigateToClienteHome() {
        getUI().ifPresent(ui -> ui.navigate("/cliente"));
    }

    private void setButtonLoading(Button b, boolean loading) {
        if (loading) {
            b.setEnabled(false);
            b.setText(tr("button.logging_in"));
            b.setIcon(VaadinIcon.SPINNER.create());
        } else {
            b.setEnabled(true);
            b.setText(tr("button.login"));
            b.setIcon(VaadinIcon.SIGN_IN.create());
        }
    }

    /* ========================= NOTIFICATIONS ========================= */

    private void notifyError(String text, Notification.Position pos) {
        Notification n = Notification.show(text);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.setPosition(pos);
    }

    private void notifySuccess(String text, Notification.Position pos) {
        Notification n = Notification.show(text);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.setPosition(pos);
    }

    /* ========================= I18N ========================= */

    private String tr(String key, Object... params) {
        return i18nProvider.getTranslation(key, UI.getCurrent().getLocale(), params);
    }

    /* ========================= SUPPORT ========================= */

    private record LocaleOption(String label, Locale locale) { }
}
