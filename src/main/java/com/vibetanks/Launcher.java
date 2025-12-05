package com.vibetanks;

/**
 * Launcher class for the application.
 * This is needed because JavaFX applications cannot be launched directly
 * from a shaded JAR when the main class extends Application.
 */
public class Launcher {
    public static void main(String[] args) {
        Main.main(args);
    }
}
