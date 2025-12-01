package com.vibetanks;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

/**
 * Manages saved IP addresses for quick reconnection.
 * Stores up to 10 most recently used IPs.
 */
public class IPHistoryManager {
    private static final String PREFS_KEY = "saved_ips";
    private static final int MAX_IPS = 10;
    private static final Preferences prefs = Preferences.userNodeForPackage(IPHistoryManager.class);

    /**
     * Get list of saved IP addresses (most recent first).
     */
    public static List<String> getSavedIPs() {
        String saved = prefs.get(PREFS_KEY, "");
        List<String> ips = new ArrayList<>();
        if (!saved.isEmpty()) {
            String[] parts = saved.split(";");
            for (String ip : parts) {
                if (!ip.trim().isEmpty()) {
                    ips.add(ip.trim());
                }
            }
        }
        return ips;
    }

    /**
     * Add an IP to the history (moves to top if already exists).
     */
    public static void addIP(String ip) {
        if (ip == null || ip.trim().isEmpty()) return;
        ip = ip.trim();

        List<String> ips = getSavedIPs();

        // Remove if already exists (will re-add at top)
        ips.remove(ip);

        // Add at beginning
        ips.add(0, ip);

        // Limit to MAX_IPS
        while (ips.size() > MAX_IPS) {
            ips.remove(ips.size() - 1);
        }

        // Save
        saveIPs(ips);
    }

    /**
     * Remove an IP from history.
     */
    public static void removeIP(String ip) {
        if (ip == null) return;

        List<String> ips = getSavedIPs();
        ips.remove(ip.trim());
        saveIPs(ips);
    }

    /**
     * Clear all saved IPs.
     */
    public static void clearAll() {
        prefs.put(PREFS_KEY, "");
    }

    private static void saveIPs(List<String> ips) {
        String joined = String.join(";", ips);
        prefs.put(PREFS_KEY, joined);
    }
}
