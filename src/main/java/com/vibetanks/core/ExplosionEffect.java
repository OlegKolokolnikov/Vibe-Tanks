package com.vibetanks.core;

/**
 * Represents an explosion animation when a tank is destroyed.
 */
public class ExplosionEffect {
    private static final int DURATION = 25; // ~0.4 seconds at 60fps

    private final double x;
    private final double y;
    private final int size;
    private int lifetime;

    public ExplosionEffect(double x, double y, int size) {
        this.x = x;
        this.y = y;
        this.size = size;
        this.lifetime = DURATION;
    }

    public void update() {
        lifetime--;
    }

    public boolean isExpired() {
        return lifetime <= 0;
    }

    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public int getLifetime() { return lifetime; }
    public int getDuration() { return DURATION; }

    /**
     * Get animation progress from 0.0 (start) to 1.0 (end).
     */
    public double getProgress() {
        return 1.0 - (double) lifetime / DURATION;
    }
}
