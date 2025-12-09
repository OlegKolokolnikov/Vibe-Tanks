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
        // Laser beam with glow effect
        double beamX = startX;
        double beamY = startY;
        double beamWidth, beamHeight;

        // Calculate beam dimensions based on direction
        switch (direction) {
            case UP -> {
                beamX = startX - BEAM_WIDTH / 2.0;
                beamY = startY - length;
                beamWidth = BEAM_WIDTH;
                beamHeight = length;
            }
            case DOWN -> {
                beamX = startX - BEAM_WIDTH / 2.0;
                beamY = startY;
                beamWidth = BEAM_WIDTH;
                beamHeight = length;
            }
            case LEFT -> {
                beamX = startX - length;
                beamY = startY - BEAM_WIDTH / 2.0;
                beamWidth = length;
                beamHeight = BEAM_WIDTH;
            }
            case RIGHT -> {
                beamX = startX;
                beamY = startY - BEAM_WIDTH / 2.0;
                beamWidth = length;
                beamHeight = BEAM_WIDTH;
            }
            default -> {
                beamWidth = 0;
                beamHeight = 0;
            }
        }

        // Outer glow (yellow/orange)
        gc.setFill(Color.rgb(255, 200, 0, 0.5));
        gc.fillRect(beamX - 6, beamY - 6, beamWidth + 12, beamHeight + 12);

        // Mid glow (orange)
        gc.setFill(Color.rgb(255, 100, 0, 0.7));
        gc.fillRect(beamX - 3, beamY - 3, beamWidth + 6, beamHeight + 6);

        // Core beam (bright red)
        gc.setFill(Color.rgb(255, 0, 0));
        gc.fillRect(beamX, beamY, beamWidth, beamHeight);

        // Inner core (white hot center)
        gc.setFill(Color.rgb(255, 255, 255, 0.9));
        double coreOffset = BEAM_WIDTH / 4.0;
        gc.fillRect(beamX + coreOffset, beamY + coreOffset,
                   Math.max(1, beamWidth - coreOffset * 2),
                   Math.max(1, beamHeight - coreOffset * 2));
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
