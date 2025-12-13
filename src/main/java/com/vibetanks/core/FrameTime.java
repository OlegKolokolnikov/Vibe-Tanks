package com.vibetanks.core;

/**
 * Utility class to cache the current frame timestamp.
 * Call updateFrameTime() once at the start of each render frame,
 * then use getFrameTime() throughout rendering to avoid multiple
 * System.currentTimeMillis() calls.
 */
public final class FrameTime {
    private FrameTime() {} // Prevent instantiation

    private static long currentFrameTime = System.currentTimeMillis();

    /**
     * Update the cached frame time. Call once at the start of each render frame.
     */
    public static void updateFrameTime() {
        currentFrameTime = System.currentTimeMillis();
    }

    /**
     * Get the cached frame time. Use this instead of System.currentTimeMillis()
     * during rendering for consistent timing and better performance.
     */
    public static long getFrameTime() {
        return currentFrameTime;
    }
}
