package com.ejyqyl.dmstudio.service.i18n;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Service for internationalization (i18n) supporting RU and EN locales.
 */
public final class I18nService {

    private static final I18nService INSTANCE = new I18nService();

    private final ObjectProperty<Locale> currentLocale = new SimpleObjectProperty<>(Locale.of("ru", "RU"));
    private ResourceBundle bundle;

    private I18nService() {
        loadBundle(currentLocale.get());
        currentLocale.addListener((obs, oldLocale, newLocale) -> loadBundle(newLocale));
    }

    public static I18nService getInstance() {
        return INSTANCE;
    }

    private void loadBundle(Locale locale) {
        this.bundle = ResourceBundle.getBundle("i18n.messages", locale);
    }

    public String get(String key, Object... args) {
        try {
            if (bundle != null && bundle.containsKey(key)) {
                String pattern = bundle.getString(key);
                if (args != null && args.length > 0) {
                    return MessageFormat.format(pattern, args);
                }
                return pattern;
            }
        } catch (Exception ignored) {
        }
        return "!" + key + "!";
    }

    public ObjectProperty<Locale> currentLocaleProperty() {
        return currentLocale;
    }

    public Locale getCurrentLocale() {
        return currentLocale.get();
    }

    public ResourceBundle getBundle() {
        return bundle;
    }

    public String getCurrentLanguage() {
        return currentLocale.get().getLanguage();
    }

    public void setLocale(Locale locale) {
        currentLocale.set(locale);
    }
}
