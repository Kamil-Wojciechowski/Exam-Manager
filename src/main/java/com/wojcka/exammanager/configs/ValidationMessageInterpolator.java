package com.wojcka.exammanager.configs;

import com.wojcka.exammanager.components.Translator;
import jakarta.validation.MessageInterpolator;
import java.util.Locale;


public class ValidationMessageInterpolator implements MessageInterpolator {

    private final MessageInterpolator defaultInterpolator;

    public ValidationMessageInterpolator(MessageInterpolator interpolator) {
        this.defaultInterpolator = interpolator;
    }
    @Override
    public String interpolate(String message, Context context) {
        message = Translator.toLocale(message);
        return defaultInterpolator.interpolate(message, context);
    }

    @Override
    public String interpolate(String message, Context context, Locale locale) {
        message = Translator.toLocale(message);
        return defaultInterpolator.interpolate(message, context, locale);
    }
}
