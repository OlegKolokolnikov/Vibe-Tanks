package com.vibetanks.util;

import javafx.scene.control.Button;

/**
 * Utility for styling buttons consistently across the UI.
 * Eliminates duplicate button styling code in MenuScene and other UI components.
 */
public class ButtonStyler {

    /**
     * Apply themed styling to a button with custom colors.
     * Sets up base style, hover effect, and exit handlers.
     *
     * @param button The button to style
     * @param baseColor Background color in normal state (e.g., "#2a5a2a")
     * @param hoverColor Background color on hover (e.g., "#3a7a3a")
     * @param textColor Text and border color (e.g., "lightgreen")
     */
    public static void applyThemedStyle(Button button, String baseColor, String hoverColor, String textColor) {
        String baseStyle = buildStyle(baseColor, textColor, "2px");
        String hoverStyle = buildStyle(hoverColor, textColor, "3px");

        button.setStyle(baseStyle);
        button.setOnMouseEntered(e -> button.setStyle(hoverStyle));
        button.setOnMouseExited(e -> button.setStyle(baseStyle));
    }

    /**
     * Apply green-themed styling (HOST GAME style).
     */
    public static void applyGreenTheme(Button button) {
        applyThemedStyle(button, "#2a5a2a", "#3a7a3a", "lightgreen");
    }

    /**
     * Apply blue-themed styling (JOIN GAME style).
     */
    public static void applyBlueTheme(Button button) {
        applyThemedStyle(button, "#2a2a5a", "#3a3a7a", "lightblue");
    }

    /**
     * Apply purple-themed styling (LEVEL EDITOR style).
     */
    public static void applyPurpleTheme(Button button) {
        applyThemedStyle(button, "#5a2a5a", "#7a3a7a", "#ff99ff");
    }

    /**
     * Apply orange-themed styling (OPTIONS style).
     */
    public static void applyOrangeTheme(Button button) {
        applyThemedStyle(button, "#4a3a2a", "#6a5a3a", "#ffcc66");
    }

    /**
     * Apply red-themed styling.
     */
    public static void applyRedTheme(Button button) {
        applyThemedStyle(button, "#5a2a2a", "#7a3a3a", "#ff9999");
    }

    /**
     * Apply cyan-themed styling.
     */
    public static void applyCyanTheme(Button button) {
        applyThemedStyle(button, "#2a5a5a", "#3a7a7a", "lightcyan");
    }

    private static String buildStyle(String backgroundColor, String textColor, String borderWidth) {
        return "-fx-background-color: " + backgroundColor + ";" +
               "-fx-text-fill: " + textColor + ";" +
               "-fx-border-color: " + textColor + ";" +
               "-fx-border-width: " + borderWidth + ";" +
               "-fx-cursor: hand;";
    }
}
