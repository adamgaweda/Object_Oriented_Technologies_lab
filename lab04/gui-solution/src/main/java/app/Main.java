package app;

import javafx.application.Application;

/**
 * Klasa startowa z main(). Nie rozszerza Application celowo: uruchomienie klasy rozszerzającej Application
 * bezpośrednio (bez Gradle) wymaga modułów JavaFX na module path, a zwykła klasa z main() uruchomi się zawsze.
 */
public class Main {

    /** Klucz Serper API (https://serper.dev -> API Keys), ten sam co w laboratorium z RxJava. */
    public static final String SERPER_API_KEY = "de4eff3cb0a7ac7e7353a49fb2e62b17bc92b3f2";

    public static void main(String[] args) {
        Application.launch(GalleryApp.class);        // startuje toolkit JavaFX, tworzy GalleryApp i woła start() na wątku JavaFX
    }
}
