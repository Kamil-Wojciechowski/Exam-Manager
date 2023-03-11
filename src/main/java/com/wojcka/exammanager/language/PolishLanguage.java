package com.wojcka.exammanager.language;

public enum PolishLanguage {
    token_not_found("Nie znaleziono tokenu!"),
    email_not_found("Nie znaleziono maila!"),
    email_already_used("Email został już użyty!")
    ;

    public final String label;

    private PolishLanguage(String label) {
        this.label = label;
    }
}
