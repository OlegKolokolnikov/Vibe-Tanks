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
    private static final double DEFAULT_PLAYER_SHOOT_SPEED = 1.0;
    private static final double DEFAULT_ENEMY_SHOOT_SPEED = 1.0;
    private static final int DEFAULT_ENEMY_COUNT = 25;

    // Current settings (saved to file)
    private static double playerSpeedMultiplier = DEFAULT_PLAYER_SPEED;
    private static double enemySpeedMultiplier = DEFAULT_ENEMY_SPEED;
    private static double playerShootSpeedMultiplier = DEFAULT_PLAYER_SHOOT_SPEED;
    private static double enemyShootSpeedMultiplier = DEFAULT_ENEMY_SHOOT_SPEED;
    private static int enemyCount = DEFAULT_ENEMY_COUNT;

    // Runtime overrides for network games (from host)
    private static boolean useHostSettings = false;
    private static double hostPlayerSpeed = DEFAULT_PLAYER_SPEED;
    private static double hostEnemySpeed = DEFAULT_ENEMY_SPEED;
    private static double hostPlayerShootSpeed = DEFAULT_PLAYER_SHOOT_SPEED;
    private static double hostEnemyShootSpeed = DEFAULT_ENEMY_SHOOT_SPEED;

    // Load settings on class initialization
    static {
        loadSettings();
    }

    /**
     * Set host settings override (called by client when receiving game state)
     */
    public static void setHostSettings(double playerSpeed, double enemySpeed,
                                       double playerShootSpeed, double enemyShootSpeed) {
        useHostSettings = true;
        hostPlayerSpeed = playerSpeed;
        hostEnemySpeed = enemySpeed;
        hostPlayerShootSpeed = playerShootSpeed;
        hostEnemyShootSpeed = enemyShootSpeed;
    }

    /**
     * Clear host settings override (called when returning to menu or starting single player)
     */
    public static void clearHostSettings() {
        useHostSettings = false;
    }

    /**
     * Check if using host settings
     */
    public static boolean isUsingHostSettings() {
        return useHostSettings;
    }

    public static double getPlayerSpeedMultiplier() {
        return useHostSettings ? hostPlayerSpeed : playerSpeedMultiplier;
    }

    public static void setPlayerSpeedMultiplier(double multiplier) {
        playerSpeedMultiplier = Math.max(0.5, Math.min(2.0, multiplier));
        saveSettings();
    }

    public static double getEnemySpeedMultiplier() {
        return useHostSettings ? hostEnemySpeed : enemySpeedMultiplier;
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

    public static double getPlayerShootSpeedMultiplier() {
        return useHostSettings ? hostPlayerShootSpeed : playerShootSpeedMultiplier;
    }

    public static void setPlayerShootSpeedMultiplier(double multiplier) {
        playerShootSpeedMultiplier = Math.max(0.5, Math.min(3.0, multiplier));
        saveSettings();
    }

    public static double getEnemyShootSpeedMultiplier() {
        return useHostSettings ? hostEnemyShootSpeed : enemyShootSpeedMultiplier;
    }

    public static void setEnemyShootSpeedMultiplier(double multiplier) {
        enemyShootSpeedMultiplier = Math.max(0.5, Math.min(3.0, multiplier));
        saveSettings();
    }

    public static void resetToDefaults() {
        playerSpeedMultiplier = DEFAULT_PLAYER_SPEED;
        enemySpeedMultiplier = DEFAULT_ENEMY_SPEED;
        playerShootSpeedMultiplier = DEFAULT_PLAYER_SHOOT_SPEED;
        enemyShootSpeedMultiplier = DEFAULT_ENEMY_SHOOT_SPEED;
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
                playerShootSpeedMultiplier = Double.parseDouble(props.getProperty("playerShootSpeed", String.valueOf(DEFAULT_PLAYER_SHOOT_SPEED)));
                enemyShootSpeedMultiplier = Double.parseDouble(props.getProperty("enemyShootSpeed", String.valueOf(DEFAULT_ENEMY_SHOOT_SPEED)));
                enemyCount = Integer.parseInt(props.getProperty("enemyCount", String.valueOf(DEFAULT_ENEMY_COUNT)));
                System.out.println("Loaded game settings: playerSpeed=" + playerSpeedMultiplier +
                                   ", enemySpeed=" + enemySpeedMultiplier +
                                   ", playerShootSpeed=" + playerShootSpeedMultiplier +
                                   ", enemyShootSpeed=" + enemyShootSpeedMultiplier +
                                   ", enemyCount=" + enemyCount);
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
            props.setProperty("playerShootSpeed", String.valueOf(playerShootSpeedMultiplier));
            props.setProperty("enemyShootSpeed", String.valueOf(enemyShootSpeedMultiplier));
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
