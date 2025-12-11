package com.vibetanks.core;

import java.util.prefs.Preferences;

/**
 * Global game settings that can be modified via menu.
 */
public class GameSettings {
    private static final Preferences prefs = Preferences.userNodeForPackage(GameSettings.class);

    // Keys for persisting settings
    private static final String KEY_PLAYER_SPEED = "player_speed";
    private static final String KEY_ENEMY_SPEED = "enemy_speed";
    private static final String KEY_PLAYER_SHOOT_SPEED = "player_shoot_speed";
    private static final String KEY_ENEMY_SHOOT_SPEED = "enemy_shoot_speed";
    private static final String KEY_SOUND_VOLUME = "sound_volume";
    private static final String KEY_MUSIC_VOLUME = "music_volume";
    private static final String KEY_ENEMY_COUNT = "enemy_count";

    // Default values
    private static final double DEFAULT_SPEED = 1.0;
    private static final double DEFAULT_VOLUME = 1.0;
    private static final int DEFAULT_ENEMY_COUNT = 25;

    // In-memory values (loaded from prefs on startup)
    private static double playerSpeedMultiplier;
    private static double enemySpeedMultiplier;
    private static double playerShootSpeedMultiplier;
    private static double enemyShootSpeedMultiplier;
    private static double soundVolume;
    private static double musicVolume;
    private static int enemyCount;

    // Host settings (for multiplayer - synced from host)
    private static Double hostPlayerSpeed = null;
    private static Double hostEnemySpeed = null;
    private static Double hostPlayerShootSpeed = null;
    private static Double hostEnemyShootSpeed = null;

    static {
        loadSettings();
    }

    private static void loadSettings() {
        playerSpeedMultiplier = prefs.getDouble(KEY_PLAYER_SPEED, DEFAULT_SPEED);
        enemySpeedMultiplier = prefs.getDouble(KEY_ENEMY_SPEED, DEFAULT_SPEED);
        playerShootSpeedMultiplier = prefs.getDouble(KEY_PLAYER_SHOOT_SPEED, DEFAULT_SPEED);
        enemyShootSpeedMultiplier = prefs.getDouble(KEY_ENEMY_SHOOT_SPEED, DEFAULT_SPEED);
        soundVolume = prefs.getDouble(KEY_SOUND_VOLUME, DEFAULT_VOLUME);
        musicVolume = prefs.getDouble(KEY_MUSIC_VOLUME, DEFAULT_VOLUME);
        enemyCount = prefs.getInt(KEY_ENEMY_COUNT, DEFAULT_ENEMY_COUNT);

        // Log loaded settings for debugging speed differences between machines
        System.out.println("[GameSettings] Loaded: playerSpeed=" + playerSpeedMultiplier +
                ", enemySpeed=" + enemySpeedMultiplier +
                ", playerShootSpeed=" + playerShootSpeedMultiplier +
                ", enemyShootSpeed=" + enemyShootSpeedMultiplier);
    }

    public static void saveSettings() {
        prefs.putDouble(KEY_PLAYER_SPEED, playerSpeedMultiplier);
        prefs.putDouble(KEY_ENEMY_SPEED, enemySpeedMultiplier);
        prefs.putDouble(KEY_PLAYER_SHOOT_SPEED, playerShootSpeedMultiplier);
        prefs.putDouble(KEY_ENEMY_SHOOT_SPEED, enemyShootSpeedMultiplier);
        prefs.putDouble(KEY_SOUND_VOLUME, soundVolume);
        prefs.putDouble(KEY_MUSIC_VOLUME, musicVolume);
        prefs.putInt(KEY_ENEMY_COUNT, enemyCount);
    }

    // Speed multipliers (0.5 = 50%, 1.0 = 100%, 2.0 = 200%)
    public static double getPlayerSpeedMultiplier() { return playerSpeedMultiplier; }
    public static void setPlayerSpeedMultiplier(double multiplier) {
        playerSpeedMultiplier = Math.max(0.25, Math.min(3.0, multiplier));
    }

    public static double getEnemySpeedMultiplier() { return enemySpeedMultiplier; }
    public static void setEnemySpeedMultiplier(double multiplier) {
        enemySpeedMultiplier = Math.max(0.25, Math.min(3.0, multiplier));
    }

    public static double getPlayerShootSpeedMultiplier() { return playerShootSpeedMultiplier; }
    public static void setPlayerShootSpeedMultiplier(double multiplier) {
        playerShootSpeedMultiplier = Math.max(0.25, Math.min(3.0, multiplier));
    }

    public static double getEnemyShootSpeedMultiplier() { return enemyShootSpeedMultiplier; }
    public static void setEnemyShootSpeedMultiplier(double multiplier) {
        enemyShootSpeedMultiplier = Math.max(0.25, Math.min(3.0, multiplier));
    }

    // Volume settings (0.0 to 1.0)
    public static double getSoundVolume() { return soundVolume; }
    public static void setSoundVolume(double volume) {
        soundVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    public static double getMusicVolume() { return musicVolume; }
    public static void setMusicVolume(double volume) {
        musicVolume = Math.max(0.0, Math.min(1.0, volume));
    }

    // Enemy count
    public static int getEnemyCount() { return enemyCount; }
    public static void setEnemyCount(int count) {
        enemyCount = Math.max(1, Math.min(100, count));
        saveSettings();
    }

    // Host settings (for multiplayer sync)
    public static void setHostSettings(double playerSpeed, double enemySpeed,
                                       double playerShootSpeed, double enemyShootSpeed) {
        hostPlayerSpeed = playerSpeed;
        hostEnemySpeed = enemySpeed;
        hostPlayerShootSpeed = playerShootSpeed;
        hostEnemyShootSpeed = enemyShootSpeed;
    }

    public static void clearHostSettings() {
        hostPlayerSpeed = null;
        hostEnemySpeed = null;
        hostPlayerShootSpeed = null;
        hostEnemyShootSpeed = null;
    }

    // Get effective settings (host overrides local in multiplayer)
    public static double getEffectivePlayerSpeed() {
        return hostPlayerSpeed != null ? hostPlayerSpeed : playerSpeedMultiplier;
    }

    public static double getEffectiveEnemySpeed() {
        return hostEnemySpeed != null ? hostEnemySpeed : enemySpeedMultiplier;
    }

    public static double getEffectivePlayerShootSpeed() {
        return hostPlayerShootSpeed != null ? hostPlayerShootSpeed : playerShootSpeedMultiplier;
    }

    public static double getEffectiveEnemyShootSpeed() {
        return hostEnemyShootSpeed != null ? hostEnemyShootSpeed : enemyShootSpeedMultiplier;
    }

    // Reset to defaults
    public static void resetToDefaults() {
        playerSpeedMultiplier = DEFAULT_SPEED;
        enemySpeedMultiplier = DEFAULT_SPEED;
        playerShootSpeedMultiplier = DEFAULT_SPEED;
        enemyShootSpeedMultiplier = DEFAULT_SPEED;
        soundVolume = DEFAULT_VOLUME;
        musicVolume = DEFAULT_VOLUME;
        enemyCount = DEFAULT_ENEMY_COUNT;
        // Don't reset nickname
        saveSettings();
    }
}
