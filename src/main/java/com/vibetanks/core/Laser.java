package com.vibetanks.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;

/**
 * Laser beam that passes through all obstacles, only damaging enemies/tanks.
 * Does not destroy terrain (except base if hit).
 * Deals 3 damage to any unit it touches.
 */
public class Laser {
    private static final int BEAM_WIDTH = GameConstants.LASER_BEAM_WIDTH;
    private static final int LIFETIME = GameConstants.LASER_LIFETIME;
    private static long nextId = 1;

    /**
     * Reset laser ID counter. Call this at level start/restart to prevent overflow.
     */
    public static void resetIdCounter() {
        nextId = 1;
    }

    private long id;
    private double startX;
    private double startY;
    private Direction direction;
    private int ownerPlayerNumber; // Which player fired this laser (0 for enemies)
    private boolean fromEnemy;
    private int lifetime;
    private double length; // Length of the beam (extends to edge of play area)

    // Play area dimensions
    private static final double PLAY_AREA_WIDTH = 26 * 32; // 832 pixels
    private static final double PLAY_AREA_HEIGHT = 26 * 32; // 832 pixels

    public Laser(double startX, double startY, Direction direction, boolean fromEnemy, int ownerPlayerNumber) {
        this.id = nextId++;
        this.startX = startX;
        this.startY = startY;
        this.direction = direction;
        this.fromEnemy = fromEnemy;
        this.ownerPlayerNumber = ownerPlayerNumber;
        this.lifetime = LIFETIME;

        // Calculate beam length to edge of play area
        calculateLength();
    }

    private void calculateLength() {
        switch (direction) {
            case UP -> length = startY; // Distance to top
            case DOWN -> length = PLAY_AREA_HEIGHT - startY - BEAM_WIDTH; // Distance to bottom
            case LEFT -> length = startX; // Distance to left
            case RIGHT -> length = PLAY_AREA_WIDTH - startX - BEAM_WIDTH; // Distance to right
        }
        length = Math.max(0, length);
    }

    public void update() {
        lifetime--;
    }

    public boolean isExpired() {
        return lifetime <= 0;
    }

    public void render(GraphicsContext gc) {
        // Thin dotted laser beam
        double dotSize = 4;      // Size of each dot
        double dotSpacing = 8;   // Space between dots (creates dotted effect)
        double thinWidth = 3;    // Thin beam width

        // Animate dots moving along the beam
        double animOffset = (System.currentTimeMillis() / 30.0) % dotSpacing;

        switch (direction) {
            case UP -> {
                double x = startX - thinWidth / 2.0;
                for (double y = startY - animOffset; y > startY - length; y -= dotSpacing) {
                    // Outer glow
                    gc.setFill(Color.rgb(255, 100, 0, 0.4));
                    gc.fillOval(x - 2, y - 2, thinWidth + 4, dotSize + 4);
                    // Core dot
                    gc.setFill(Color.rgb(255, 50, 0, 0.9));
                    gc.fillOval(x, y, thinWidth, dotSize);
                    // Hot center
                    gc.setFill(Color.rgb(255, 200, 100, 0.8));
                    gc.fillOval(x + 0.5, y + 0.5, thinWidth - 1, dotSize - 1);
                }
            }
            case DOWN -> {
                double x = startX - thinWidth / 2.0;
                for (double y = startY + animOffset; y < startY + length; y += dotSpacing) {
                    gc.setFill(Color.rgb(255, 100, 0, 0.4));
                    gc.fillOval(x - 2, y - 2, thinWidth + 4, dotSize + 4);
                    gc.setFill(Color.rgb(255, 50, 0, 0.9));
                    gc.fillOval(x, y, thinWidth, dotSize);
                    gc.setFill(Color.rgb(255, 200, 100, 0.8));
                    gc.fillOval(x + 0.5, y + 0.5, thinWidth - 1, dotSize - 1);
                }
            }
            case LEFT -> {
                double y = startY - thinWidth / 2.0;
                for (double x = startX - animOffset; x > startX - length; x -= dotSpacing) {
                    gc.setFill(Color.rgb(255, 100, 0, 0.4));
                    gc.fillOval(x - 2, y - 2, dotSize + 4, thinWidth + 4);
                    gc.setFill(Color.rgb(255, 50, 0, 0.9));
                    gc.fillOval(x, y, dotSize, thinWidth);
                    gc.setFill(Color.rgb(255, 200, 100, 0.8));
                    gc.fillOval(x + 0.5, y + 0.5, dotSize - 1, thinWidth - 1);
                }
            }
            case RIGHT -> {
                double y = startY - thinWidth / 2.0;
                for (double x = startX + animOffset; x < startX + length; x += dotSpacing) {
                    gc.setFill(Color.rgb(255, 100, 0, 0.4));
                    gc.fillOval(x - 2, y - 2, dotSize + 4, thinWidth + 4);
                    gc.setFill(Color.rgb(255, 50, 0, 0.9));
                    gc.fillOval(x, y, dotSize, thinWidth);
                    gc.setFill(Color.rgb(255, 200, 100, 0.8));
                    gc.fillOval(x + 0.5, y + 0.5, dotSize - 1, thinWidth - 1);
                }
            }
        }
    }

    /**
     * Check if laser beam intersects with a tank
     */
    public boolean collidesWith(Tank tank) {
        double tankX = tank.getX();
        double tankY = tank.getY();
        double tankSize = tank.getSize();

        // Get beam bounding box
        double beamLeft, beamRight, beamTop, beamBottom;

        switch (direction) {
            case UP -> {
                beamLeft = startX - BEAM_WIDTH / 2.0;
                beamRight = startX + BEAM_WIDTH / 2.0;
                beamTop = startY - length;
                beamBottom = startY;
            }
            case DOWN -> {
                beamLeft = startX - BEAM_WIDTH / 2.0;
                beamRight = startX + BEAM_WIDTH / 2.0;
                beamTop = startY;
                beamBottom = startY + length;
            }
            case LEFT -> {
                beamLeft = startX - length;
                beamRight = startX;
                beamTop = startY - BEAM_WIDTH / 2.0;
                beamBottom = startY + BEAM_WIDTH / 2.0;
            }
            case RIGHT -> {
                beamLeft = startX;
                beamRight = startX + length;
                beamTop = startY - BEAM_WIDTH / 2.0;
                beamBottom = startY + BEAM_WIDTH / 2.0;
            }
            default -> {
                return false;
            }
        }

        // Check rectangle intersection
        return beamLeft < tankX + tankSize &&
               beamRight > tankX &&
               beamTop < tankY + tankSize &&
               beamBottom > tankY;
    }

    /**
     * Check if laser beam intersects with UFO
     */
    public boolean collidesWithUFO(UFO ufo) {
        if (ufo == null || !ufo.isAlive() || fromEnemy) return false;

        double ufoX = ufo.getX();
        double ufoY = ufo.getY();
        double ufoSize = 48; // UFO size

        // Get beam bounding box
        double beamLeft, beamRight, beamTop, beamBottom;

        switch (direction) {
            case UP -> {
                beamLeft = startX - BEAM_WIDTH / 2.0;
                beamRight = startX + BEAM_WIDTH / 2.0;
                beamTop = startY - length;
                beamBottom = startY;
            }
            case DOWN -> {
                beamLeft = startX - BEAM_WIDTH / 2.0;
                beamRight = startX + BEAM_WIDTH / 2.0;
                beamTop = startY;
                beamBottom = startY + length;
            }
            case LEFT -> {
                beamLeft = startX - length;
                beamRight = startX;
                beamTop = startY - BEAM_WIDTH / 2.0;
                beamBottom = startY + BEAM_WIDTH / 2.0;
            }
            case RIGHT -> {
                beamLeft = startX;
                beamRight = startX + length;
                beamTop = startY - BEAM_WIDTH / 2.0;
                beamBottom = startY + BEAM_WIDTH / 2.0;
            }
            default -> {
                return false;
            }
        }

        // Check rectangle intersection
        return beamLeft < ufoX + ufoSize &&
               beamRight > ufoX &&
               beamTop < ufoY + ufoSize &&
               beamBottom > ufoY;
    }

    /**
     * Check if laser beam hits the base
     */
    public boolean collidesWithBase(Base base) {
        double baseX = base.getX();
        double baseY = base.getY();
        double baseSize = 64; // Base is 2x2 tiles

        // Get beam bounding box
        double beamLeft, beamRight, beamTop, beamBottom;

        switch (direction) {
            case UP -> {
                beamLeft = startX - BEAM_WIDTH / 2.0;
                beamRight = startX + BEAM_WIDTH / 2.0;
                beamTop = startY - length;
                beamBottom = startY;
            }
            case DOWN -> {
                beamLeft = startX - BEAM_WIDTH / 2.0;
                beamRight = startX + BEAM_WIDTH / 2.0;
                beamTop = startY;
                beamBottom = startY + length;
            }
            case LEFT -> {
                beamLeft = startX - length;
                beamRight = startX;
                beamTop = startY - BEAM_WIDTH / 2.0;
                beamBottom = startY + BEAM_WIDTH / 2.0;
            }
            case RIGHT -> {
                beamLeft = startX;
                beamRight = startX + length;
                beamTop = startY - BEAM_WIDTH / 2.0;
                beamBottom = startY + BEAM_WIDTH / 2.0;
            }
            default -> {
                return false;
            }
        }

        // Check rectangle intersection
        return beamLeft < baseX + baseSize &&
               beamRight > baseX &&
               beamTop < baseY + baseSize &&
               beamBottom > baseY;
    }

    // Getters
    public long getId() { return id; }
    public double getStartX() { return startX; }
    public double getStartY() { return startY; }
    public Direction getDirection() { return direction; }
    public int getOwnerPlayerNumber() { return ownerPlayerNumber; }
    public boolean isFromEnemy() { return fromEnemy; }
    public int getLifetime() { return lifetime; }
    public double getLength() { return length; }

    // For network sync
    public void setId(long id) { this.id = id; }
}
