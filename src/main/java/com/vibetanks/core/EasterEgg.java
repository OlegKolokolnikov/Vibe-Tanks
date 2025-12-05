package com.vibetanks.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Easter egg collectible that spawns when UFO is killed.
 * When collected by a player:
 * - All enemy tanks (except BOSS) turn into rainbow/POWER tanks
 * - The collecting player gets 3 extra lives
 * When collected by an enemy:
 * - All enemy tanks (except BOSS) turn into HEAVY (black) tanks
 */
public class EasterEgg {
    private static final int SIZE = 32;
    private static final int LIFETIME = 600; // 10 seconds at 60 FPS

    private double x;
    private double y;
    private int lifetime;
    private boolean collected;

    public EasterEgg(double x, double y) {
        this.x = x;
        this.y = y;
        this.lifetime = LIFETIME;
        this.collected = false;
    }

    public void update() {
        lifetime--;
    }

    public boolean isExpired() {
        return lifetime <= 0 || collected;
    }

    public boolean collidesWith(Tank tank) {
        return x < tank.getX() + tank.getSize() &&
               x + SIZE > tank.getX() &&
               y < tank.getY() + tank.getSize() &&
               y + SIZE > tank.getY();
    }

    public void collect() {
        collected = true;
    }

    public boolean isCollected() {
        return collected;
    }

    public void render(GraphicsContext gc) {
        // Pulsing/glowing effect
        double pulse = (Math.sin(System.currentTimeMillis() / 100.0) + 1) / 2; // 0 to 1

        // Draw egg shape with rainbow gradient effect
        double eggWidth = SIZE * 0.7;
        double eggHeight = SIZE * 0.9;
        double eggX = x + (SIZE - eggWidth) / 2;
        double eggY = y + (SIZE - eggHeight) / 2;

        // Outer glow
        gc.setFill(Color.rgb(255, 255, 100, 0.3 + pulse * 0.3));
        gc.fillOval(eggX - 4, eggY - 4, eggWidth + 8, eggHeight + 8);

        // Egg base (white/cream)
        gc.setFill(Color.rgb(255, 250, 220));
        gc.fillOval(eggX, eggY, eggWidth, eggHeight);

        // Rainbow stripes on the egg
        double stripeHeight = eggHeight / 6;
        Color[] rainbowColors = {
            Color.RED, Color.ORANGE, Color.YELLOW,
            Color.GREEN, Color.BLUE, Color.PURPLE
        };

        gc.save();
        // Clip to egg shape
        gc.beginPath();
        gc.arc(eggX + eggWidth / 2, eggY + eggHeight / 2, eggWidth / 2, eggHeight / 2, 0, 360);
        gc.closePath();
        gc.clip();

        for (int i = 0; i < 6; i++) {
            // Shift colors based on time for animation
            int colorIndex = (i + (int)(System.currentTimeMillis() / 200)) % 6;
            gc.setFill(rainbowColors[colorIndex]);
            gc.fillRect(eggX, eggY + i * stripeHeight, eggWidth, stripeHeight + 1);
        }
        gc.restore();

        // Egg outline
        gc.setStroke(Color.rgb(200, 180, 100));
        gc.setLineWidth(2);
        gc.strokeOval(eggX, eggY, eggWidth, eggHeight);

        // Sparkle effect
        if (pulse > 0.7) {
            gc.setFill(Color.WHITE);
            gc.fillOval(eggX + eggWidth * 0.3, eggY + eggHeight * 0.2, 4, 4);
        }
    }

    // Getters
    public double getX() { return x; }
    public double getY() { return y; }
    public int getLifetime() { return lifetime; }

    // Setters for network sync
    public void setPosition(double x, double y) {
        this.x = x;
        this.y = y;
    }
    public void setLifetime(int lifetime) { this.lifetime = lifetime; }
}
