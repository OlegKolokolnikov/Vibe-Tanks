package com.vibetanks.core;

import com.vibetanks.util.GameLogger;

import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/**
 * Manages the player's nickname.
 * Stores nickname persistently using Java Preferences.
 */
public class NicknameManager {
    private static final GameLogger LOG = GameLogger.getLogger(NicknameManager.class);
    private static final String PREFS_KEY = "player_nickname";
    private static final Preferences prefs = Preferences.userNodeForPackage(NicknameManager.class);
    private static final int MAX_NICKNAME_LENGTH = 12;

    /**
     * Get the saved nickname, or null if not set.
     */
    public static String getNickname() {
        String nickname = prefs.get(PREFS_KEY, null);
        if (nickname != null && nickname.trim().isEmpty()) {
            return null;
        }
        return nickname;
    }

    /**
     * Save the player's nickname.
     */
    public static void setNickname(String nickname) {
        if (nickname == null || nickname.trim().isEmpty()) {
            prefs.remove(PREFS_KEY);
            flushPrefs();
            return;
        }

        // Trim and limit length
        nickname = nickname.trim();
        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            nickname = nickname.substring(0, MAX_NICKNAME_LENGTH);
        }

        prefs.put(PREFS_KEY, nickname);
        flushPrefs();
        LOG.info("Nickname saved: {}", nickname);
    }

    /**
     * Flush preferences to persistent storage.
     */
    private static void flushPrefs() {
        try {
            prefs.flush();
        } catch (BackingStoreException e) {
            LOG.error("Failed to save nickname to preferences: {}", e.getMessage());
        }
    }

    /**
     * Clear the saved nickname.
     */
    public static void clearNickname() {
        prefs.remove(PREFS_KEY);
        flushPrefs();
        LOG.info("Nickname cleared");
    }

    /**
     * Get the display name for a player.
     * Returns the nickname if this is the local player and has a nickname,
     * otherwise returns "P" + playerNumber.
     */
    public static String getDisplayName(int playerNumber, boolean isLocalPlayer) {
        if (isLocalPlayer) {
            String nickname = getNickname();
            if (nickname != null) {
                return nickname;
            }
        }
        return "P" + playerNumber;
    }

    /**
     * Get maximum allowed nickname length.
     */
    public static int getMaxLength() {
        return MAX_NICKNAME_LENGTH;
    }
}
