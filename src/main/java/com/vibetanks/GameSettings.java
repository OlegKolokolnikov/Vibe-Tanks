package com.vibetanks;

import java.io.*;
import java.util.Properties;

/**
 * Stores game settings that can be configured in the options menu.
 * Settings are persisted to a file.
 */
public class GameSettings {
    private static final String SETTINGS_FILE = "game_settings.properties";

    // Default values
    private static final double DEFAULT_PLAYER_SPEED = 1.0;
    private static final double DEFAULT_ENEMY_SPEED = 1.0;
    private static final int DEFAULT_ENEMY_COUNT = 25;

    // Current settings
    private static double playerSpeedMultiplier = DEFAULT_PLAYER_SPEED;
    private static double enemySpeedMultiplier = DEFAULT_ENEMY_SPEED;
    private static int enemyCount = DEFAULT_ENEMY_COUNT;

    // Load settings on class initialization
    static {
        loadSettings();
    }

    public static double getPlayerSpeedMultiplier() {
        return playerSpeedMultiplier;
    }

    public static void setPlayerSpeedMultiplier(double multiplier) {
        playerSpeedMultiplier = Math.max(0.5, Math.min(2.0, multiplier));
        saveSettings();
    }

    public static double getEnemySpeedMultiplier() {
        return enemySpeedMultiplier;
    }

    public static void setEnemySpeedMultiplier(double multiplier) {
        enemySpeedMultiplier = Math.max(0.5, Math.min(2.0, multiplier));
        saveSettings();
    }

    public static int getEnemyCount() {
        return enemyCount;
    }

    public static void setEnemyCount(int count) {
        enemyCount = Math.max(5, Math.min(100, count));
        saveSettings();
    }

    public static void resetToDefaults() {
        playerSpeedMultiplier = DEFAULT_PLAYER_SPEED;
        enemySpeedMultiplier = DEFAULT_ENEMY_SPEED;
        enemyCount = DEFAULT_ENEMY_COUNT;
        saveSettings();
    }

    private static void loadSettings() {
        try {
            File file = new File(SETTINGS_FILE);
            if (file.exists()) {
                Properties props = new Properties();
                try (FileInputStream fis = new FileInputStream(file)) {
                    props.load(fis);
                }
                playerSpeedMultiplier = Double.parseDouble(props.getProperty("playerSpeed", String.valueOf(DEFAULT_PLAYER_SPEED)));
                enemySpeedMultiplier = Double.parseDouble(props.getProperty("enemySpeed", String.valueOf(DEFAULT_ENEMY_SPEED)));
                enemyCount = Integer.parseInt(props.getProperty("enemyCount", String.valueOf(DEFAULT_ENEMY_COUNT)));
                System.out.println("Loaded game settings: playerSpeed=" + playerSpeedMultiplier +
                                   ", enemySpeed=" + enemySpeedMultiplier + ", enemyCount=" + enemyCount);
            }
        } catch (Exception e) {
            System.out.println("Could not load settings, using defaults: " + e.getMessage());
        }
    }

    private static void saveSettings() {
        try {
            Properties props = new Properties();
            props.setProperty("playerSpeed", String.valueOf(playerSpeedMultiplier));
            props.setProperty("enemySpeed", String.valueOf(enemySpeedMultiplier));
            props.setProperty("enemyCount", String.valueOf(enemyCount));

            try (FileOutputStream fos = new FileOutputStream(SETTINGS_FILE)) {
                props.store(fos, "Vibe Tanks Game Settings");
            }
            System.out.println("Saved game settings");
        } catch (Exception e) {
            System.out.println("Could not save settings: " + e.getMessage());
        }
    }
}
