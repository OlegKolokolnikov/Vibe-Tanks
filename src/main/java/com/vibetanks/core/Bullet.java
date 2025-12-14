package com.vibetanks.core;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import java.util.concurrent.atomic.AtomicLong;

public class Bullet {
    private static final int DEFAULT_SIZE = GameConstants.BULLET_SIZE;
    private static final double SPEED = GameConstants.BULLET_SPEED;
    private static final AtomicLong nextId = new AtomicLong(1); // Thread-safe ID counter

    /**
     * Reset bullet ID counter. Call this at level start/restart to prevent overflow.
     */
    public static void resetIdCounter() {
        nextId.set(1);
    }

    private long id;
    private double x;
    private double y;
    private Direction direction;
    private boolean fromEnemy;
    private int power;
    private boolean canDestroyTrees;
    private int ownerPlayerNumber; // 1-4 for player bullets, 0 for enemy
    private int size; // Bullet size (bigger for BOSS)

    public Bullet(double x, double y, Direction direction, boolean fromEnemy, int power, boolean canDestroyTrees) {
        this(x, y, direction, fromEnemy, power, canDestroyTrees, 0, DEFAULT_SIZE);
    }

    public Bullet(double x, double y, Direction direction, boolean fromEnemy, int power, boolean canDestroyTrees, int ownerPlayerNumber) {
        this(x, y, direction, fromEnemy, power, canDestroyTrees, ownerPlayerNumber, DEFAULT_SIZE);
    }

    public Bullet(double x, double y, Direction direction, boolean fromEnemy, int power, boolean canDestroyTrees, int ownerPlayerNumber, int size) {
        this.id = nextId.getAndIncrement();
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.fromEnemy = fromEnemy;
        this.power = power;
        this.canDestroyTrees = canDestroyTrees;
        this.ownerPlayerNumber = ownerPlayerNumber;
        this.size = size;
    }

    // Constructor with explicit ID (for network sync)
    public Bullet(long id, double x, double y, Direction direction, boolean fromEnemy, int power, boolean canDestroyTrees, int ownerPlayerNumber, int size) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.direction = direction;
        this.fromEnemy = fromEnemy;
        this.power = power;
        this.canDestroyTrees = canDestroyTrees;
        this.ownerPlayerNumber = ownerPlayerNumber;
        this.size = size;
    }

    public void update() {
        x += direction.getDx() * SPEED;
        y += direction.getDy() * SPEED;
    }

    public void render(GraphicsContext gc) {
        if (fromEnemy) {
            gc.setFill(Color.RED);
        } else {
            // Use player's tank color for their bullets
            gc.setFill(Tank.getPlayerColor(ownerPlayerNumber));
        }
        gc.fillOval(x, y, size, size);
    }

    public boolean collidesWith(Tank tank) {
        return Collider.checkSquare(x, y, size, tank.getX(), tank.getY(), tank.getSize());
    }

    public boolean collidesWith(Base base) {
        return Collider.checkSquare(x, y, size, base.getX(), base.getY(), base.getSize());
    }

    public boolean collidesWith(Bullet other) {
        return Collider.checkSquare(x, y, size, other.x, other.y, other.size);
    }

    public boolean isOutOfBounds(int width, int height) {
        return x < 0 || x > width || y < 0 || y > height;
    }

    // Check if bullet is out of bounds
    // Returns true if bullet should continue, false if it should be removed
    public boolean handleWraparound(GameMap map, int width, int height) {
        // Remove bullets that go off-screen (no wrapping allowed)
        if (x < 0 || x > width || y < 0 || y > height) {
            return false; // Bullet is out of bounds, remove it
        }

        return true; // Bullet is within bounds
    }

    public long getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }
    public int getSize() { return size; }
    public Direction getDirection() { return direction; }
    public boolean isFromEnemy() { return fromEnemy; }
    public int getPower() { return power; }
    public boolean canDestroyTrees() { return canDestroyTrees; }
    public int getOwnerPlayerNumber() { return ownerPlayerNumber; }
}
