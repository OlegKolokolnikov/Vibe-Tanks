package com.vibetanks.core;

import java.util.prefs.Preferences;

/**
 * Manages the player's nickname.
 * Stores nickname persistently using Java Preferences.
 */
public class NicknameManager {
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
            return;
        }

        // Trim and limit length
        nickname = nickname.trim();
        if (nickname.length() > MAX_NICKNAME_LENGTH) {
            nickname = nickname.substring(0, MAX_NICKNAME_LENGTH);
        }

        prefs.put(PREFS_KEY, nickname);
    }

    /**
     * Clear the saved nickname.
     */
    public static void clearNickname() {
        prefs.remove(PREFS_KEY);
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
